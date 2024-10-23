package analyseur;

import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class SpoonCouplageAnalyse {
    private Map<String, Set<String>> declaredMethods;
    private Map<String, Set<String>> methodCalls;
    private Set<String> allClasses;
    private Map<String, Map<String, List<String>>> relations;
    private CtModel model;
    private static int MAX_MODULES;
    private static double CP;

    public SpoonCouplageAnalyse() {
        this.declaredMethods = new HashMap<>();
        this.methodCalls = new HashMap<>();
        this.allClasses = new HashSet<>();
        this.relations = new HashMap<>();
    }

    public static void main(String[] args) {
        SpoonCouplageAnalyse analyser = new SpoonCouplageAnalyse();
        Scanner scanner = new Scanner(System.in);

        System.out.print("Veuillez entrer le chemin du projet : ");
        String projectPath = scanner.nextLine();

        analyser.analyseProject(projectPath);
        analyser.calculateCouplingMetrics();
        analyser.hierarchicalClustering();

        System.out.print("Voulez-vous construire le graphe de couplage (oui/non) ? ");
        String buildGraph = scanner.nextLine().trim().toLowerCase();

        if (buildGraph.equals("oui")) {
            analyser.buildCouplingGraph();
            System.out.print("Voulez-vous construire le fichier PNG (oui/non) ? ");
            String buildPng = scanner.nextLine().trim().toLowerCase();
            if (buildPng.equals("oui")) {
                analyser.generatePng();
            }
        }

        scanner.close();
    }

    public void analyseProject(String projectPath) {
        // Configuration de Spoon
        Launcher launcher = new Launcher();
        launcher.addInputResource(projectPath);
        launcher.getEnvironment().setNoClasspath(true);
        launcher.buildModel();
        this.model = launcher.getModel();

        // Analyse des classes et méthodes
        extractClassesAndMethods();
        analyseMethodCalls();

        // Affichage des résultats
        printAnalysisResults();
    }

    private void extractClassesAndMethods() {
        for (CtClass<?> ctClass : model.getElements(new TypeFilter<>(CtClass.class))) {
            String className = ctClass.getSimpleName();
            allClasses.add(className);
            declaredMethods.putIfAbsent(className, new HashSet<>());

            // Extraction des méthodes déclarées
            for (CtMethod<?> method : ctClass.getMethods()) {
                String methodSignature = method.getSimpleName() + getParametersSignature(method);
                declaredMethods.get(className).add(methodSignature);
            }
        }
    }

    private String getParametersSignature(CtMethod<?> method) {
        StringBuilder signature = new StringBuilder("(");
        method.getParameters().forEach(param -> {
            if (signature.length() > 1) signature.append(", ");
            signature.append(param.getType().getSimpleName());
        });
        signature.append(")");
        return signature.toString();
    }

    private void analyseMethodCalls() {
        for (CtClass<?> ctClass : model.getElements(new TypeFilter<>(CtClass.class))) {
            String className = ctClass.getSimpleName();
            methodCalls.putIfAbsent(className, new HashSet<>());

            // Analyse des appels de méthodes
            List<CtInvocation<?>> invocations = ctClass.getElements(new TypeFilter<>(CtInvocation.class));
            for (CtInvocation<?> invocation : invocations) {
                CtExecutableReference<?> executable = invocation.getExecutable();
                CtTypeReference<?> declaringType = executable.getDeclaringType();
                
                if (declaringType != null) {
                    String targetClass = declaringType.getSimpleName();
                    if (allClasses.contains(targetClass) && !targetClass.equals(className)) {
                        String methodCall = executable.getSimpleName() + "()";
                        methodCalls.get(className).add(methodCall);
                    }
                }
            }
        }
    }

    private void printAnalysisResults() {
        System.out.println("\n--- Méthodes déclarées pour chaque classe ---");
        declaredMethods.forEach((className, methods) -> {
            System.out.println("Classe: " + className);
            methods.forEach(method -> System.out.println("  - " + method));
        });

        System.out.println("\n--- Appels de méthodes pour chaque classe ---");
        methodCalls.forEach((className, calls) -> {
            System.out.println("Classe: " + className);
            calls.forEach(call -> System.out.println("  - " + call));
        });
    }

    public void calculateCouplingMetrics() {
        // Initialisation des relations
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

        Set<String> displayedRelations = new HashSet<>();
        int totalRelations = calculateTotalRelations();

        // Affichage des relations et métriques
        printRelationsAndMetrics(displayedRelations, totalRelations);
    }

    private int calculateTotalRelations() {
        Set<String> counted = new HashSet<>();
        int total = 0;
        
        for (String classA : relations.keySet()) {
            for (String classB : relations.get(classA).keySet()) {
                String relationKey = classA.compareTo(classB) < 0 
                    ? classA + "-" + classB 
                    : classB + "-" + classA;
                    
                if (!counted.contains(relationKey)) {
                    total += relations.get(classA).get(classB).size();
                    counted.add(relationKey);
                }
            }
        }
        
        return total;
    }

    private void printRelationsAndMetrics(Set<String> displayedRelations, int totalRelations) {
        System.out.println("\n--- Relations entre les classes ---");
        for (String classA : relations.keySet()) {
            for (Map.Entry<String, List<String>> entry : relations.get(classA).entrySet()) {
                String classB = entry.getKey();
                String relationKey = classA + "-" + classB;

                if (!displayedRelations.contains(relationKey)) {
                    List<String> relationMethods = entry.getValue();
                    System.out.printf("Relation entre %s et %s: %d, Méthodes: %s%n",
                            classA, classB, relationMethods.size(), relationMethods);
                    displayedRelations.add(relationKey);
                    displayedRelations.add(classB + "-" + classA);
                }
            }
        }

        System.out.println("\nTotal des relations: " + totalRelations);

        // Affichage des métriques de couplage
        displayedRelations.clear();
        System.out.println("\n--- Métriques de couplage ---");
        for (String classA : relations.keySet()) {
            for (Map.Entry<String, List<String>> entry : relations.get(classA).entrySet()) {
                String classB = entry.getKey();
                String relationKey = classA + "-" + classB;

                if (!displayedRelations.contains(relationKey)) {
                    double metric = (double) entry.getValue().size() / totalRelations;
                    System.out.printf("Métrique de couplage entre %s et %s: %.2f%n",
                            classA, classB, metric);
                    displayedRelations.add(relationKey);
                    displayedRelations.add(classB + "-" + classA);
                }
            }
        }
    }

    private List<String> getRelationMethods(String classA, String classB) {
        List<String> relationMethods = new ArrayList<>();
        
        if (methodCalls.containsKey(classA)) {
            for (String methodCall : methodCalls.get(classA)) {
                if (isDeclaredMethod(classB, methodCall)) {
                    relationMethods.add(methodCall);
                }
            }
        }

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

        String methodName = methodCall.substring(0, methodCall.indexOf('('));
        return declaredMethods.get(className).stream()
            .anyMatch(method -> method.startsWith(methodName + "("));
    }

    public void buildCouplingGraph() {
        StringBuilder dotContent = new StringBuilder("graph Couplage {\n");
        Set<String> displayedRelations = new HashSet<>();
        int totalRelations = calculateTotalRelations();

        for (String classA : relations.keySet()) {
            for (Map.Entry<String, List<String>> entry : relations.get(classA).entrySet()) {
                String classB = entry.getKey();
                String relationKey = classA + "-" + classB;
                
                if (!displayedRelations.contains(relationKey)) {
                    int relationCount = entry.getValue().size();
                    String label = String.format("%d / %d", relationCount, totalRelations);
                    
                    dotContent.append("  ").append(classA).append(" -- ").append(classB)
                            .append(" [label=\"").append(label).append("\"];\n");
                            
                    displayedRelations.add(relationKey);
                    displayedRelations.add(classB + "-" + classA);
                }
            }
        }

        dotContent.append("}\n");
        writeDotFile(dotContent.toString());
    }

    private void writeDotFile(String content) {
        File resultsDir = new File("Results");
        resultsDir.mkdir();

        try {
            Files.write(Paths.get("Results/coupling_graph.dot"), content.getBytes());
            System.out.println("Fichier .dot créé : Results/coupling_graph.dot");
        } catch (IOException e) {
            System.err.println("Erreur lors de l'écriture du fichier .dot : " + e.getMessage());
        }
    }

    public void generatePng() {
        try {
            ProcessBuilder pb = new ProcessBuilder("dot", "-Tpng", 
                "Results/coupling_graph.dot", "-o", "Results/coupling_graph.png");
            Process process = pb.start();
            process.waitFor();
            System.out.println("Fichier PNG créé : Results/coupling_graph.png");
        } catch (IOException | InterruptedException e) {
            System.err.println("Erreur lors de la génération du fichier PNG : " + e.getMessage());
        }
    }

    public void hierarchicalClustering() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Veuillez entrer le nombre maximal de modules (MAX_MODULES) : ");
        MAX_MODULES = scanner.nextInt();
        System.out.print("Veuillez entrer la moyenne de couplage minimum (CP) : ");
        CP = scanner.nextDouble();

        List<Set<String>> clusters = initializeClusters();
        performClustering(clusters);
        printFinalClusters(clusters);
    }

    private List<Set<String>> initializeClusters() {
        List<Set<String>> clusters = new ArrayList<>();
        for (String className : allClasses) {
            Set<String> cluster = new HashSet<>();
            cluster.add(className);
            clusters.add(cluster);
        }
        return clusters;
    }

    private void performClustering(List<Set<String>> clusters) {
        while (clusters.size() > MAX_MODULES) {
            double maxCoupling = -1;
            Set<String> bestClusterA = null;
            Set<String> bestClusterB = null;

            for (int i = 0; i < clusters.size(); i++) {
                for (int j = i + 1; j < clusters.size(); j++) {
                    double couplingMetric = calculateAverageCoupling(clusters.get(i), clusters.get(j));
                    if (couplingMetric > maxCoupling) {
                        maxCoupling = couplingMetric;
                        bestClusterA = clusters.get(i);
                        bestClusterB = clusters.get(j);
                    }
                }
            }

            if (maxCoupling > CP && bestClusterA != null && bestClusterB != null) {
                bestClusterA.addAll(bestClusterB);
                clusters.remove(bestClusterB);
            } else {
                break;
            }
        }
    }

    private void printFinalClusters(List<Set<String>> clusters) {
        System.out.println("\n--- Modules regroupés ---");
        for (int i = 0; i < clusters.size(); i++) {
            System.out.printf("Module %d : %s%n", i + 1, clusters.get(i));
        }
    }

    private double calculateAverageCoupling(Set<String> clusterA, Set<String> clusterB) {
        double totalCoupling = 0;
        int count = 0;

        for (String classA : clusterA) {
            for (String classB : clusterB) {
                List<String> relationMethods = getRelationMethods(classA, classB);
                totalCoupling += relationMethods.size();
                count++;
            }
        }

        return count == 0 ? 0 : totalCoupling / count;
    }
}