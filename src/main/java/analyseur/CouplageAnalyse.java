package analyseur;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CouplageAnalyse {
    private Map<String, Set<String>> declaredMethods = new HashMap<>();
    private Map<String, Set<String>> methodCalls = new HashMap<>();
    private Set<String> allClasses = new HashSet<>();
    private Map<String, Map<String, List<String>>> relations = new HashMap<>();
    private static int MAX_MODULES;
    private static double CP;

    public static void main(String[] args) {
        CouplageAnalyse analyser = new CouplageAnalyse();
        Scanner scanner = new Scanner(System.in);

        System.out.print("Veuillez entrer le chemin du projet : ");
        String projectPath = scanner.nextLine();

        analyser.analyseProject(projectPath);
        analyser.calculateCouplingMetrics();
        analyser.hierarchicalClustering(); // Appel de la méthode de clustering


        // Demander si l'utilisateur veut construire le graphe
        System.out.print("Voulez-vous construire le graphe de couplage (oui/non) ? ");
        String buildGraph = scanner.nextLine().trim().toLowerCase();

        if (buildGraph.equals("oui")) {
            analyser.buildCouplingGraph();
            // Demander si l'utilisateur veut construire le fichier PNG
            System.out.print("Voulez-vous construire le fichier PNG (oui/non) ? ");
            String buildPng = scanner.nextLine().trim().toLowerCase();
            if (buildPng.equals("oui")) {
                analyser.generatePng();
            }
        }

        scanner.close();
    }

    public void analyseProject(String projectPath) {
        try {
            // Phase 1: Extraction des méthodes déclarées dans chaque classe
            Files.walk(Paths.get(projectPath))
                .filter(Files::isRegularFile)
                .filter(file -> file.toString().endsWith(".java"))
                .forEach(this::extractDeclaredMethods);

            // Affichage des méthodes déclarées pour chaque classe
            System.out.println("\n--- Méthodes déclarées pour chaque classe ---");
            declaredMethods.forEach((className, methods) -> {
                System.out.println("Classe: " + className);
                methods.forEach(method -> System.out.println("  - " + method));
            });

            // Phase 2: Analyse des appels de méthodes entre classes
            Files.walk(Paths.get(projectPath))
                .filter(Files::isRegularFile)
                .filter(file -> file.toString().endsWith(".java"))
                .forEach(this::analyseMethodCalls);

            // Affichage des appels de méthodes pour chaque classe
            System.out.println("\n--- Appels de méthodes pour chaque classe ---");
            methodCalls.forEach((className, calls) -> {
                System.out.println("Classe: " + className);
                calls.forEach(call -> System.out.println("  - " + call));
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void extractDeclaredMethods(java.nio.file.Path filePath) {
        try {
            String className = filePath.getFileName().toString().replace(".java", "");
            allClasses.add(className);
            List<String> lines = Files.readAllLines(filePath);

            declaredMethods.putIfAbsent(className, new HashSet<>());
            for (String line : lines) {
                String methodSignature = extractMethodSignature(line);
                if (methodSignature != null) {
                    declaredMethods.get(className).add(methodSignature);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void analyseMethodCalls(java.nio.file.Path filePath) {
        try {
            String className = filePath.getFileName().toString().replace(".java", "");
            List<String> lines = Files.readAllLines(filePath);

            methodCalls.putIfAbsent(className, new HashSet<>());

            for (String line : lines) {
                // Détection des appels de méthodes dans la ligne
                for (String calledClass : allClasses) {
                    if (!calledClass.equals(className)) {
                        // On recherche d'abord les appels de méthode à partir d'une instance
                        Set<String> callsFromInstance = extractMethodCalls(line, calledClass);
                        for (String call : callsFromInstance) {
                            methodCalls.get(className).add(call);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String extractMethodSignature(String line) {
        String regex = "(?:public|protected|private|static|\\s) +[\\w\\<\\>\\[\\]]+\\s+(\\w+)\\s*\\([^\\)]*\\)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            return matcher.group(1) + matcher.group().substring(matcher.group().indexOf('('));
        }
        return null;
    }

    private Set<String> extractMethodCalls(String line, String calledClass) {
        Set<String> calls = new HashSet<>();
        String regex = "\\b(\\w+)\\s*\\(.*\\)\\s*;?"; // Trouve tous les appels de méthodes
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(line);

        while (matcher.find()) {
            String methodName = matcher.group(1);
            calls.add(methodName + "()");
        }

        return calls;
    }

    public void calculateCouplingMetrics() {
        // Initialise les relations entre les classes
        for (String classA : allClasses) {
            relations.putIfAbsent(classA, new HashMap<>());

            for (String classB : allClasses) {
                if (!classA.equals(classB)) {
                    List<String> relationMethods = getRelationMethods(classA, classB);
                    if (!relationMethods.isEmpty()) {
                        relations.get(classA).put(classB, relationMethods);
                    }
                }
            }
        }

        // Set pour suivre les relations affichées
        Set<String> displayedRelations = new HashSet<>();
        int totalRelations = 0;  // Initialiser le total des relations

        // Affichage des relations
        System.out.println("\n--- Relations entre les classes ---");
        for (String classA : relations.keySet()) {
            for (Map.Entry<String, List<String>> entry : relations.get(classA).entrySet()) {
                String classB = entry.getKey();
                String relationKey = classA + "-" + classB; // clé unique pour la relation

                if (!displayedRelations.contains(relationKey)) {
                    List<String> relationMethods = entry.getValue();
                    int relationCount = relationMethods.size();

                    // Ajout à totalRelations
                    totalRelations += relationCount;

                    // Affichage de la relation
                    System.out.printf("Relation entre %s et %s: %d, Méthodes: %s%n",
                            classA, classB, relationCount, relationMethods);

                    // Marquer la relation comme affichée
                    displayedRelations.add(relationKey);
                    displayedRelations.add(classB + "-" + classA); // Ajout pour éviter la duplication
                }
            }
        }

        // Affichage du total des relations
        System.out.println("Total des relations: " + totalRelations);

        // Calculer et afficher la métrique de couplage pour chaque relation unique
        System.out.println("\n--- Métriques de couplage ---");
        for (String classA : relations.keySet()) {
            for (Map.Entry<String, List<String>> entry : relations.get(classA).entrySet()) {
                String classB = entry.getKey();
                String relationKey = classA + "-" + classB; // clé unique pour la relation

                // Vérifiez si la relation a déjà été affichée
                if (displayedRelations.contains(relationKey)) {
                    List<String> relationMethods = entry.getValue();
                    int relationCount = relationMethods.size();

                    // Calculer la métrique de couplage en utilisant totalRelations
                    double metric = (double) relationCount / totalRelations;

                    // Affichage de la métrique uniquement si non affichée auparavant
                    System.out.printf("Métrique de couplage entre %s et %s: %.2f%n",
                            classA, classB, metric);

                    // Marquer comme affichée pour éviter la duplication
                    displayedRelations.add(relationKey);
                }
            }
        }
    }

    private List<String> getRelationMethods(String classA, String classB) {
        List<String> relationMethods = new ArrayList<>();

        // Vérifiez les appels de méthodes de classA vers classB
        if (methodCalls.containsKey(classA)) {
            for (String methodCall : methodCalls.get(classA)) {
                if (isDeclaredMethod(classB, methodCall)) {
                    relationMethods.add(methodCall);
                }
            }
        }

        // Vérifiez les appels de méthodes de classB vers classA
        if (methodCalls.containsKey(classB)) {
            for (String methodCall : methodCalls.get(classB)) {
                if (isDeclaredMethod(classA, methodCall)) {
                    relationMethods.add(methodCall);
                }
            }
        }

        return relationMethods;
    }

    private boolean isDeclaredMethod(String className, String methodCall) {
        if (!declaredMethods.containsKey(className)) {
            return false;
        }

        // Recherche exacte
        if (declaredMethods.get(className).contains(methodCall)) {
            return true;
        }

        // Recherche sans paramètres
        String methodName = methodCall.substring(0, methodCall.indexOf('('));
        for (String declaredMethod : declaredMethods.get(className)) {
            if (declaredMethod.startsWith(methodName + "(")) {
                return true;
            }
        }

        return false;
    }

    private void buildCouplingGraph() {
        StringBuilder dotContent = new StringBuilder("graph Couplage {\n");

        // Utilisation d'un ensemble pour suivre les relations ajoutées
        Set<String> displayedRelations = new HashSet<>();
        int totalRelations = 0;  // Initialiser totalRelations

        // Première passe pour calculer totalRelations
        for (String classA : relations.keySet()) {
            for (Map.Entry<String, List<String>> entry : relations.get(classA).entrySet()) {
                String classB = entry.getKey();
                String relationKeyAB = classA + "-" + classB;
                String relationKeyBA = classB + "-" + classA;

                // Vérification pour ne pas ajouter la relation si elle a déjà été affichée
                if (!displayedRelations.contains(relationKeyAB) && !displayedRelations.contains(relationKeyBA)) {
                    List<String> relationMethods = entry.getValue();
                    int relationCount = relationMethods.size();

                    // Ajouter relationCount à totalRelations une seule fois
                    totalRelations += relationCount; 

                    // Marquer la relation comme affichée
                    displayedRelations.add(relationKeyAB); // Ajout uniquement dans une direction
                }
            }
        }

        // Réinitialiser l'ensemble des relations affichées pour le calcul des métriques
        displayedRelations.clear();

        // Deuxième passe pour construire le graphe et calculer les métriques
        for (String classA : relations.keySet()) {
            for (Map.Entry<String, List<String>> entry : relations.get(classA).entrySet()) {
                String classB = entry.getKey();
                String relationKeyAB = classA + "-" + classB;
                String relationKeyBA = classB + "-" + classA;

                // Vérification pour ne pas ajouter la relation si elle a déjà été affichée
                if (!displayedRelations.contains(relationKeyAB) && !displayedRelations.contains(relationKeyBA)) {
                    List<String> relationMethods = entry.getValue();
                    int relationCount = relationMethods.size();

                    // Calculer la métrique pour la relation
                    double metric = (double) relationCount / totalRelations;

                    // Créer le label pour l'arête
                    String label = String.format("%d / %d", relationCount, totalRelations);

                    // Ajouter l'arête avec le label
                    dotContent.append("  ").append(classA).append(" -- ").append(classB)
                            .append(" [label=\"").append(label).append("\"];\n");

                    // Marquer la relation comme affichée
                    displayedRelations.add(relationKeyAB); // Ajout uniquement dans une direction
                }
            }
        }

        dotContent.append("}\n");

        // Création du dossier Results
        File resultsDir = new File("Results");
        if (!resultsDir.exists()) {
            resultsDir.mkdir();
        }

        // Écriture dans le fichier .dot
        String dotFilePath = "Results/coupling_graph.dot";
        try {
            Files.write(Paths.get(dotFilePath), dotContent.toString().getBytes());
            System.out.println("Fichier .dot créé : " + dotFilePath);
        } catch (IOException e) {
            System.err.println("Erreur lors de l'écriture du fichier .dot : " + e.getMessage());
        }

        // Affichage du total des relations
        System.out.println("Total des relations: " + totalRelations);
    }


    private void generatePng() {
        String dotFilePath = "Results/coupling_graph.dot";
        String pngFilePath = "Results/coupling_graph.png";

        // Utiliser Graphviz pour générer le fichier PNG
        try {
            ProcessBuilder pb = new ProcessBuilder("dot", "-Tpng", dotFilePath, "-o", pngFilePath);
            Process process = pb.start();
            process.waitFor();
            System.out.println("Fichier PNG créé : " + pngFilePath);
        } catch (IOException | InterruptedException e) {
            System.err.println("Erreur lors de la génération du fichier PNG : " + e.getMessage());
        }
    }
    
    public void hierarchicalClustering() {
        // Paramètres d'entrée
        Scanner scanner = new Scanner(System.in);
        System.out.print("Veuillez entrer le nombre maximal de modules (MAX_MODULES) : ");
        MAX_MODULES = scanner.nextInt();
        System.out.print("Veuillez entrer la moyenne de couplage minimum (CP) : ");
        CP = scanner.nextDouble();

        // Liste pour stocker les clusters
        List<Set<String>> clusters = new ArrayList<>();
        List<String> clusterNames = new ArrayList<>(); // Pour stocker les noms des clusters en fonction des classes regroupées

        // Initialiser chaque classe comme un cluster
        for (String className : allClasses) {
            Set<String> cluster = new HashSet<>();
            cluster.add(className);
            clusters.add(cluster);
            clusterNames.add(className); // Initialement, chaque cluster porte le nom de la classe unique
        }

        StringBuilder dotContent = new StringBuilder("digraph HierarchicalClustering {\n");

        // Construction du graphe hiérarchique en ajoutant des clusters
        while (clusters.size() > MAX_MODULES) {
            double maxCoupling = -1;
            int bestClusterAIndex = -1;
            int bestClusterBIndex = -1;

            // Trouver les deux clusters les plus couplés
            for (int i = 0; i < clusters.size(); i++) {
                for (int j = i + 1; j < clusters.size(); j++) {
                    double couplingMetric = calculateAverageCoupling(clusters.get(i), clusters.get(j));
                    if (couplingMetric > maxCoupling) {
                        maxCoupling = couplingMetric;
                        bestClusterAIndex = i;
                        bestClusterBIndex = j;
                    }
                }
            }

            if (maxCoupling < CP) {
                break;
            }

            // Fusion des clusters
            Set<String> bestClusterA = clusters.get(bestClusterAIndex);
            Set<String> bestClusterB = clusters.get(bestClusterBIndex);
            
            // Nouveau nom pour le cluster fusionné, basé sur les noms des classes dans les deux clusters
            String newClusterName = "{" + clusterNames.get(bestClusterAIndex) + ", " + clusterNames.get(bestClusterBIndex) + "}";
            
            // Ajouter la fusion au graphe
            dotContent.append("  ").append("\"").append(newClusterName).append("\" -> ")
                      .append("\"").append(clusterNames.get(bestClusterAIndex)).append("\";\n");
            dotContent.append("  ").append("\"").append(newClusterName).append("\" -> ")
                      .append("\"").append(clusterNames.get(bestClusterBIndex)).append("\";\n");

            // Fusionner les deux clusters
            bestClusterA.addAll(bestClusterB);
            clusters.remove(bestClusterBIndex);
            clusterNames.set(bestClusterAIndex, newClusterName);
            clusterNames.remove(bestClusterBIndex);
        }

        dotContent.append("}\n");

        // Sauvegarder le fichier .dot
        String dotFilePath = "Results/hierarchical_clustering_graph.dot";
        try {
            Files.write(Paths.get(dotFilePath), dotContent.toString().getBytes());
            System.out.println("Fichier .dot de clustering hiérarchique créé : " + dotFilePath);
        } catch (IOException e) {
            System.err.println("Erreur lors de l'écriture du fichier .dot de clustering hiérarchique : " + e.getMessage());
        }

        // Générer le fichier PNG du graphe de clustering hiérarchique
        generatePng(dotFilePath, "Results/hierarchical_clustering_graph.png");
    }



    private void buildClusteringGraph(List<Set<String>> clusters) {
        StringBuilder dotContent = new StringBuilder("graph Clustering {\n");

        int moduleNumber = 1;
        for (Set<String> cluster : clusters) {
            for (String className : cluster) {
                dotContent.append("  \"").append(className).append("\" -- \"Module ").append(moduleNumber).append("\";\n");
            }
            moduleNumber++;
        }

        dotContent.append("}\n");

        // Créer le fichier .dot pour le graphe de clustering
        String dotFilePath = "Results/clustering_graph.dot";
        try {
            Files.write(Paths.get(dotFilePath), dotContent.toString().getBytes());
            System.out.println("Fichier .dot de clustering créé : " + dotFilePath);
        } catch (IOException e) {
            System.err.println("Erreur lors de l'écriture du fichier .dot de clustering : " + e.getMessage());
        }

        // Générer le fichier PNG du graphe de clustering
        generatePng("Results/clustering_graph.dot", "Results/clustering_graph.png");
    }

    private void generatePng(String dotFilePath, String pngFilePath) {
        try {
            ProcessBuilder pb = new ProcessBuilder("dot", "-Tpng", dotFilePath, "-o", pngFilePath);
            Process process = pb.start();
            process.waitFor();
            System.out.println("Fichier PNG de clustering créé : " + pngFilePath);
        } catch (IOException | InterruptedException e) {
            System.err.println("Erreur lors de la génération du fichier PNG de clustering : " + e.getMessage());
        }
    }


    private double calculateAverageCoupling(Set<String> clusterA, Set<String> clusterB) {
        double totalCoupling = 0;
        int count = 0;

        for (String classA : clusterA) {
            for (String classB : clusterB) {
                List<String> relationMethods = getRelationMethods(classA, classB);
                totalCoupling += relationMethods.size(); // Nombre de méthodes appelées
                count++;
            }
        }

        return (count == 0) ? 0 : totalCoupling / count; // Eviter la division par zéro
    }

}

