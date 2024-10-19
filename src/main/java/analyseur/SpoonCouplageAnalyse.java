package analyseur;

import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.*;

public class SpoonCouplageAnalyse {
    private Map<String, Set<String>> declaredMethods = new HashMap<>();
    private Map<String, Set<String>> methodCalls = new HashMap<>();
    private Set<String> allClasses = new HashSet<>();

    public static void main(String[] args) {
        SpoonCouplageAnalyse analyser = new SpoonCouplageAnalyse();
        Scanner scanner = new Scanner(System.in);

        System.out.print("Veuillez entrer le chemin du projet : ");
        String projectPath = scanner.nextLine();

        analyser.analyseProjectWithSpoon(projectPath);
        analyser.calculateCouplingMetrics();
    }

    public void analyseProjectWithSpoon(String projectPath) {
        // Initialisation de Spoon
        Launcher launcher = new Launcher();
        launcher.getEnvironment().setComplianceLevel(19);  // Spécifier la version Java
        launcher.getEnvironment().setNoClasspath(true);   // Ignorer le classpath si nécessaire
        launcher.addInputResource(projectPath);
        launcher.buildModel();
        CtModel model = launcher.getModel();

        // Parcourir toutes les classes trouvées dans le modèle
        for (CtClass<?> ctClass : model.getElements(new TypeFilter<>(CtClass.class))) {
            String className = ctClass.getQualifiedName(); // Utiliser le nom qualifié complet
            allClasses.add(className);

            // Extraction des méthodes déclarées dans la classe
            Set<String> methodsInClass = new HashSet<>();
            for (CtMethod<?> method : ctClass.getMethods()) {
                methodsInClass.add(ctClass.getQualifiedName() + "#" + method.getSimpleName() + method.getSignature());
            }
            declaredMethods.put(className, methodsInClass);

            // Extraction des appels de méthodes dans la classe
            Set<String> callsInClass = new HashSet<>();
            for (CtInvocation<?> invocation : ctClass.getElements(new TypeFilter<>(CtInvocation.class))) {
                String calledMethod = invocation.getExecutable().getDeclaringType().getQualifiedName() 
                    + "#" + invocation.getExecutable().getSimpleName();
                callsInClass.add(calledMethod + "()");
            }
            methodCalls.put(className, callsInClass);
        }

        // Affichage des méthodes déclarées pour chaque classe
        System.out.println("\n--- Méthodes déclarées pour chaque classe ---");
        declaredMethods.forEach((className, methods) -> {
            System.out.println("Classe: " + className);
            methods.forEach(method -> System.out.println("  - " + method));
        });

        // Affichage des appels de méthodes pour chaque classe
        System.out.println("\n--- Appels de méthodes pour chaque classe ---");
        methodCalls.forEach((className, calls) -> {
            System.out.println("Classe: " + className);
            calls.forEach(call -> System.out.println("  - " + call));
        });
    }

    public void calculateCouplingMetrics() {
        // Initialiser les relations entre classes
        for (String classA : allClasses) {
            for (String classB : allClasses) {
                if (!classA.equals(classB)) {
                    List<String> relationMethods = getRelationMethods(classA, classB);
                    if (!relationMethods.isEmpty()) {
                        System.out.printf("Relation entre %s et %s: %d méthodes: %s%n", 
                                classA, classB, relationMethods.size(), relationMethods);
                    }
                }
            }
        }
    }

    private List<String> getRelationMethods(String classA, String classB) {
        List<String> relationMethods = new ArrayList<>();

        // Vérifier les appels de méthodes de classA vers classB
        if (methodCalls.containsKey(classA)) {
            for (String methodCall : methodCalls.get(classA)) {
                if (isDeclaredMethod(classB, methodCall)) {
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
        return declaredMethods.get(className).contains(methodCall);
    }
}

