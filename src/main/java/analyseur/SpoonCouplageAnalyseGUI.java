package analyseur;

import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.visitor.filter.TypeFilter;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class SpoonCouplageAnalyseGUI {
    private Map<String, Set<String>> declaredMethods = new HashMap<>();
    private Map<String, Set<String>> methodCalls = new HashMap<>();
    private Set<String> allClasses = new HashSet<>();
    private Map<String, Map<String, Integer>> couplingGraph = new HashMap<>();
    
    private JFrame frame;
    private JTextField projectPathField;
    private JTextField cpField;
    private JTextArea outputArea;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SpoonCouplageAnalyseGUI::new);
    }

    public SpoonCouplageAnalyseGUI() {
        frame = new JFrame("Analyse de Couplage avec Spoon");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        
        JLabel projectPathLabel = new JLabel("Chemin du projet :");
        projectPathField = new JTextField(30);
        JLabel cpLabel = new JLabel("Moyenne de couplage (CP) :");
        cpField = new JTextField(5);
        JButton analyseButton = new JButton("Analyser");
        JButton displayGraphButton = new JButton("Afficher le graphe de couplage");
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        
        JPanel panel = new JPanel();
        panel.add(projectPathLabel);
        panel.add(projectPathField);
        panel.add(cpLabel);
        panel.add(cpField);
        panel.add(analyseButton);
        panel.add(displayGraphButton);
        
        analyseButton.addActionListener(e -> {
            String projectPath = projectPathField.getText();
            double cpThreshold = cpField.getText().isEmpty() ? 0.0 : Double.parseDouble(cpField.getText());

            outputArea.setText("");
            analyseProjectWithSpoon(projectPath);
            calculateCouplingGraph();
            displayModuleGrouping(cpThreshold);
        });
        
        displayGraphButton.addActionListener(e -> displayCouplingGraph());
        
        frame.getContentPane().add(panel, BorderLayout.NORTH);
        frame.getContentPane().add(new JScrollPane(outputArea), BorderLayout.CENTER);
        frame.setVisible(true);
    }

    public void analyseProjectWithSpoon(String projectPath) {
        declaredMethods.clear();
        methodCalls.clear();
        allClasses.clear();
        
        Launcher launcher = new Launcher();
        launcher.getEnvironment().setComplianceLevel(19);
        launcher.getEnvironment().setNoClasspath(true);
        launcher.addInputResource(projectPath);
        launcher.buildModel();
        CtModel model = launcher.getModel();

        for (CtClass<?> ctClass : model.getElements(new TypeFilter<>(CtClass.class))) {
            String className = ctClass.getQualifiedName();
            allClasses.add(className);

            Set<String> methodsInClass = new HashSet<>();
            for (CtMethod<?> method : ctClass.getMethods()) {
                methodsInClass.add(ctClass.getQualifiedName() + "#" + method.getSimpleName() + method.getSignature());
            }
            declaredMethods.put(className, methodsInClass);

            Set<String> callsInClass = new HashSet<>();
            for (CtInvocation<?> invocation : ctClass.getElements(new TypeFilter<>(CtInvocation.class))) {
                String calledMethod = invocation.getExecutable().getDeclaringType().getQualifiedName() 
                    + "#" + invocation.getExecutable().getSimpleName();
                callsInClass.add(calledMethod + "()");
            }
            methodCalls.put(className, callsInClass);
        }

        outputArea.append("\n--- Analyse du projet terminé ---\n");
    }

    public void calculateCouplingGraph() {
        couplingGraph.clear();

        for (String classA : allClasses) {
            couplingGraph.putIfAbsent(classA, new HashMap<>());
            for (String classB : allClasses) {
                if (!classA.equals(classB)) {
                    int couplingScore = calculateCouplingBetween(classA, classB);
                    if (couplingScore > 0) {
                        couplingGraph.get(classA).put(classB, couplingScore);
                    }
                }
            }
        }

        outputArea.append("\n--- Graphe de couplage ---\n");
        couplingGraph.forEach((classA, edges) -> {
            edges.forEach((classB, score) -> 
                outputArea.append(String.format("Couplage entre %s et %s : %d\n", classA, classB, score))
            );
        });
    }

    private int calculateCouplingBetween(String classA, String classB) {
        int score = 0;

        if (methodCalls.containsKey(classA)) {
            for (String methodCall : methodCalls.get(classA)) {
                if (isDeclaredMethod(classB, methodCall)) {
                    score++;
                }
            }
        }

        return score;
    }

    private boolean isDeclaredMethod(String className, String methodCall) {
        return declaredMethods.containsKey(className) && declaredMethods.get(className).contains(methodCall);
    }

    public void displayModuleGrouping(double cpThreshold) {
        Map<String, Set<String>> moduleGroups = new HashMap<>();
        Set<String> visited = new HashSet<>();

        for (String classA : couplingGraph.keySet()) {
            if (!visited.contains(classA)) {
                Set<String> module = new HashSet<>();
                module.add(classA);
                visited.add(classA);

                for (Map.Entry<String, Integer> entry : couplingGraph.get(classA).entrySet()) {
                    String classB = entry.getKey();
                    double avgCoupling = entry.getValue() / 2.0;  // Calcul de la moyenne de couplage

                    if (avgCoupling >= cpThreshold && !visited.contains(classB)) {
                        module.add(classB);
                        visited.add(classB);
                    }
                }

                moduleGroups.put("Module" + (moduleGroups.size() + 1), module);
            }
        }

        outputArea.append("\n--- Groupement de Modules basé sur le seuil CP ---\n");
        moduleGroups.forEach((moduleName, classes) -> {
            outputArea.append(moduleName + " : " + classes + "\n");
        });
    }

    private void displayCouplingGraph() {
        StringBuilder dotContent = new StringBuilder("digraph CouplingGraph {\n");

        couplingGraph.forEach((classA, edges) -> {
            edges.forEach((classB, score) -> 
                dotContent.append(String.format("\"%s\" -> \"%s\" [label=\"%d\"];\n", classA, classB, score))
            );
        });

        dotContent.append("}");

        File dotFile = new File("couplingGraph.dot");
        try (FileWriter writer = new FileWriter(dotFile)) {
            writer.write(dotContent.toString());
            writer.flush();

            outputArea.append("\n--- Graphe de couplage enregistré sous format DOT ---\n");

            Process process = Runtime.getRuntime().exec("dot -Tpng couplingGraph.dot -o couplingGraph.png");
            process.waitFor();
            outputArea.append("\n--- Graphe de couplage généré en image ---\n");
            Desktop.getDesktop().open(new File("couplingGraph.png"));
        } catch (IOException | InterruptedException e) {
            outputArea.append("Erreur lors de la génération du graphe de couplage : " + e.getMessage() + "\n");
        }
    }
}
