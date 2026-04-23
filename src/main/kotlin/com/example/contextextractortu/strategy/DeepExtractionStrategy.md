# Stratégie Automate : Deep Extraction (Implémentation)

## 1. Architecture Implémentée

### Composants Principaux

| Composant | Fichier | Rôle |
|-----------|---------|------|
| `DeepExtractionStrategy` | `strategy/DeepExtractionStrategy.kt` | Orchestrateur principal de l'automate en 3 étapes |
| `DeepExtractionResult` | `model/DeepExtractionResult.kt` | Modèle de données du résultat complet |
| `DeepExtractionResultMapper` | `strategy/DeepExtractionResultMapper.kt` | Adapter le résultat vers le système de templates |
| `ClassClassification` | `util/ClassClassification.kt` | Module de classification Internes/Externes/Exclusions |
| `PsiScanner` | `collector/PsiScanner.kt` | Extraction des appels de méthodes via PSI |

### Flux de Données

```
GetContextAction (UI)
    │
    └── ContextSearcher.gather()
            │
            ├── DeepExtractionStrategy.execute()
            │       │
            │       ├── Étape A : analyzeTargetMethod()
            │       ├── Étape B : resolveInstantiation()
            │       └── Étape C : performDeepScan()
            │               │
            │               ├── processCalls() (récursif)
            │               ├── resolveInternalMethod()
            │               └── resolveExternalMethod()
            │
            └── DeepExtractionResultMapper.map()
                    │
                    └── GenericContextModel → Template → Prompt final
```

## 2. Implémentation des Étapes

### Étape A : Analyse de la Méthode Cible

**Fonction**: `analyzeTargetMethod(method: PsiMethod): TargetMethodInfo`

Extrait :
- Signature complète (modificateurs, type de retour, nom, paramètres)
- Corps complet (source code)
- Annotations de la méthode
- Champs utilisés dans la méthode (analyse récursive du PSI)
- Paramètres d'entrée avec leurs types et annotations

**Exemple de sortie** :
```kotlin
TargetMethodInfo(
    methodName = "processOrder",
    signature = "public OrderResult processOrder(OrderRequest request)",
    sourceCode = "public OrderResult processOrder(...) { ... }",
    returnType = "OrderResult",
    parameters = [MethodParameter("request", "OrderRequest", [])],
    annotations = ["@Transactional"],
    fieldsUsed = [FieldUsage("orderRepository", "OrderRepository")]
)
```

### Étape B : Résolution de l'Instanciation

**Fonction**: `resolveInstantiation(psiClass: PsiClass, targetMethod: TargetMethodInfo): InstantiationPlan`

Pour la classe cible et sa hiérarchie :
1. Collecte tous les champs (classe + super-classes)
2. Identifie les constructeurs disponibles
3. Priorise le constructeur `@Autowired` ou le plus complet
4. Calcule les setters nécessaires pour les champs non couverts

**Exemple de sortie** :
```kotlin
InstantiationPlan(
    targetClass = "com.example.OrderService",
    constructor = ConstructorChoice(
        name = "OrderService",
        parameters = [
            ConstructorParameter("orderRepository", "OrderRepository", ["@Autowired"]),
            ConstructorParameter("paymentClient", "PaymentClient", ["@Autowired"])
        ],
        isAutowired = true
    ),
    settersNeeded = [],
    fieldsToInitialize = [
        FieldInfo("orderRepository", "OrderRepository", ["@Autowired"]),
        FieldInfo("paymentClient", "PaymentClient", ["@Autowired"])
    ]
)
```

### Étape C : Analyse du Flux (Deep Scan)

**Fonction**: `performDeepScan(targetMethod: PsiMethod, parentClass: PsiClass): DeepScanResult`

Implémente la logique de classification :

#### Condition 1 : Méthode Interne
- **Définition** : Méthode de la même classe ou d'une super-classe
- **Action** : Récupération du corps complet
- **Récursivité** : Analyse des appels dans cette méthode (boucle retour à Étape C)
- **Profondeur** : Limitée à 10 niveaux (limite de sécurité)

#### Condition 2 : Méthode Externe
- **Définition** : Méthode d'une autre classe du projet
- **Action** : Récupération de la signature uniquement
- **Interface** : Si la classe possède une interface, signature via l'interface
- **Mock** : Préparation des informations pour le mocking (constructeurs, setters)

#### Condition 3 : Exclusion
- **Définition** : Classes système (`java.*`, `javax.*`), primitives, wrappers, collections
- **Action** : Ignorer l'analyse, utiliser tel quel dans le test

**Classes exclues** :
```kotlin
PRIMITIVES = {int, long, double, float, boolean, char, byte, short}
JAVA_LANG_WRAPPERS = {Integer, Long, Double, Float, Boolean, Character, Byte, Short, String, Object, ...}
COMMON_COLLECTIONS = {List, Set, Map, Collection, Iterator, ArrayList, HashMap, ...}
COMMON_EXCLUDED = {Optional, Stream, LocalDate, LocalTime, ...}
```

## 3. Classification des Classes

**Module**: `ClassClassification`

```kotlin
sealed class ClassificationResult {
    data class Excluded(val className: String) : ClassificationResult()
    data class Internal(val className: String, val psiClass: PsiClass) : ClassificationResult()
    data class External(val className: String, val psiClass: PsiClass?, val isDataClass: Boolean) : ClassificationResult()
}
```

**Règles de classification** :
- `Excluded` : `java.*`, `javax.*`, primitives, wrappers, collections courantes
- `Internal` : Même classe ou super-classe de la méthode cible (déterminé au niveau de la stratégie)
- `External` : Toutes les autres classes du projet

**Détection des Data Objects** (DTO/Entity) :
- Annotations : `@DTO`, `@Entity`, `@Document`, `@Data`, `@ValueObject`
- Conventions de nommage : `*DTO`, `*Entity`, `*Request`, `*Response`, `*Command`, `*Query`

## 4. Sorties Produites

Le `DeepExtractionResult` produit les 4 sorties exigées :

1. **Code source méthode cible + méthodes internes appelées**
   - `targetMethod.sourceCode` : Corps complet de la méthode
   - `internalMethods[].body` : Corps complet de chaque méthode interne

2. **Signatures des méthodes externes à mocker**
   - `externalMethods[].signature` : Signature complète de chaque méthode externe
   - `externalMethods[].interfaceSignature` : Signature via interface si disponible

3. **Plan d'instanciation du SUT**
   - `instantiationPlan.constructor` : Constructeur à utiliser
   - `instantiationPlan.settersNeeded` : Setters nécessaires
   - `instantiationPlan.fieldsToInitialize` : Tous les champs

4. **Liste des objets complexes à créer**
   - `complexObjects[]` : Instructions de construction pour chaque paramètre complexe

## 5. Extensibilité

### Ajouter une Nouvelle Stratégie

1. Implémenter `ContextStrategy` interface
2. Définir `id` unique
3. Implémenter `execute(project, editor, scanner): GenericContextModel`

### Personnaliser la Classification

Hériter ou composer `ClassClassification` pour :
- Ajouter des règles d'exclusion
- Modifier les critères de data object detection
- Adapter les règles Internes/Externes

### Étendre le Mapper

`DeepExtractionResultMapper` peut être étendu pour :
- Produire différents formats de sortie
- Ajouter de nouveaux context items
- Adapter aux besoins spécifiques des templates

## 6. Template Updated

Le template `deep-unit-test.md` a été mis à jour pour inclure toutes les nouvelles sections :
- `targetMethodSignature` : Signature complète
- `methodAnnotations` : Annotations de la méthode
- `methodParameters` : Paramètres d'entrée
- `instantiationPlan` : Plan d'instanciation
- `classFields` : Champs de la classe
- `constructor` : Constructeur à utiliser
- `settersNeeded` : Setters nécessaires
- `internalMethods` : Méthodes internes avec corps complet
- `externalMethods` : Méthodes externes avec signatures
- `externalDependencies` : Dépendances à mocker
- `complexObjects` : Objets complexes à créer
- `excludedReferences` : Références exclues
