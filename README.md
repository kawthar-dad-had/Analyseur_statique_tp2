
# CouplageAnalyse

## Description

Ce projet est un analyseur de couplage entre classes Java, permettant d'extraire et d'analyser les relations entre classes à partir des méthodes déclarées et appelées. L'analyse est ensuite utilisée pour calculer des métriques de couplage et effectuer un **clustering hiérarchique** basé sur les interactions entre les classes. Un graphe de couplage peut également être généré pour visualiser ces relations.

## Fonctionnalités

- **Extraction des méthodes déclarées et des appels de méthodes :**
  - Extraction des signatures des méthodes déclarées dans chaque classe Java.
  - Analyse des appels de méthodes entre classes.
  
- **Analyse du couplage entre classes :**
  - Calcul des relations entre classes (nombre d'appels de méthodes).
  - Calcul des métriques de couplage entre classes.
  - Génération d'un fichier `.dot` décrivant un graphe de couplage.

- **Génération d'un graphe visuel :**
  - Construction d'un graphe de couplage au format PNG en utilisant Graphviz.

- **Clustering hiérarchique :**
  - Regroupement des classes en modules en fonction de leur couplage.
  - Paramètres personnalisés pour le nombre maximal de modules et la moyenne de couplage minimale.

## Pré-requis

- **Eclipse IDE** pour le développement et l'exécution du projet.
- **Java 8+** (ou une version plus récente).
- **Graphviz** installé pour générer des fichiers PNG à partir de fichiers `.dot`.

## Installation et Exécution

1. **Cloner le projet ou importer dans Eclipse :**

   - Clonez ce dépôt ou téléchargez les fichiers source.
   - Importez le projet dans **Eclipse** :
     - Ouvrez Eclipse.
     - Allez dans **File > Import > Existing Projects into Workspace**.
     - Sélectionnez le répertoire contenant le projet.

2. **Exécution du programme :**

   - Ouvrez la classe `CouplageAnalyse` dans **Eclipse**.
   - Exécutez-la en tant qu'application Java (`Run As > Java Application`).
   - Suivez les instructions dans la console pour entrer le chemin du projet à analyser et d'autres paramètres.

3. **Génération du graphe de couplage :**

   Si vous avez installé **Graphviz** et choisi de générer le graphe :

   - Un fichier `.dot` sera créé dans le dossier `Results`.
   - Si vous avez sélectionné la génération de PNG, un fichier `coupling_graph.png` sera également généré dans le même dossier.

## Utilisation

1. **Analyse du projet :**
   - Entrez le chemin du projet Java à analyser.
   - Le programme extrait les méthodes et les appels de méthodes, puis affiche les relations entre classes et calcule les métriques de couplage.

2. **Clustering hiérarchique :**
   - Entrez le nombre maximal de modules (**MAX_MODULES**) et la moyenne de couplage minimale (**CP**) pour définir le regroupement des classes en modules.

3. **Génération du graphe de couplage :**
   - Le programme peut générer un fichier `.dot` représentant les relations entre classes.
   - Vous pouvez choisir de générer un fichier PNG à partir du fichier `.dot` si **Graphviz** est installé.

## Exemple de sortie

### Relations entre classes :
```
--- Relations entre les classes ---
Relation entre ClasseA et ClasseB: 3, Méthodes: [method1(), method2(), method3()]
Relation entre ClasseB et ClasseC: 2, Méthodes: [methodX(), methodY()]
Total des relations: 5
```

### Métrique de couplage :
```
--- Métriques de couplage ---
Métrique de couplage entre ClasseA et ClasseB: 0.60
Métrique de couplage entre ClasseB et ClasseC: 0.40
```

### Clustering hiérarchique :
```
Veuillez entrer le nombre maximal de modules (MAX_MODULES) : 3
Veuillez entrer la moyenne de couplage minimum (CP) : 0.5
--- Modules regroupés ---
Module : [ClasseA, ClasseB]
Module : [ClasseC]
```

## Structure des dossiers

```
/src
  /analyseur
    - CouplageAnalyse.java  // Fichier principal contenant l'analyse et le clustering
/Results
  - coupling_graph.dot      // Fichier DOT pour le graphe de couplage
  - coupling_graph.png      // Graphe généré en PNG (si sélectionné)
```

## Notes supplémentaires

- Vous pouvez personnaliser les paramètres de clustering en modifiant **MAX_MODULES** et **CP** directement dans le programme ou via l'entrée utilisateur.
- Assurez-vous que **Graphviz** est correctement installé pour générer des graphes PNG à partir des fichiers DOT.


