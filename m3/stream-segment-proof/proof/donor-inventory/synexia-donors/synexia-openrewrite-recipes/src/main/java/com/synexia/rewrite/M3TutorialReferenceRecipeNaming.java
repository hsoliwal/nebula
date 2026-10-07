// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.Objects;

/** Stable names for source-wide, aspect-wide, and supported source×aspect tutorial recipes. */
public final class M3TutorialReferenceRecipeNaming {
  private static final String PREFIX = "com.synexia.rewrite.";

  private M3TutorialReferenceRecipeNaming() {}

  public static String sourceRecipeName(M3TutorialReferenceCatalog.SourceId sourceId) {
    return PREFIX + "M3TutorialSource" + sourceSuffix(sourceId);
  }

  public static String aspectRecipeName(M3TutorialReferenceCatalog.Aspect aspect) {
    return PREFIX + "M3TutorialAspect" + aspectSuffix(aspect);
  }

  public static String pairRecipeName(
      M3TutorialReferenceCatalog.SourceId sourceId,
      M3TutorialReferenceCatalog.Aspect aspect) {
    return PREFIX + "M3TutorialPair" + sourceSuffix(sourceId) + aspectSuffix(aspect);
  }

  public static String sourceSuffix(M3TutorialReferenceCatalog.SourceId sourceId) {
    return switch (Objects.requireNonNull(sourceId, "sourceId")) {
      case JAVA2S -> "Java2s";
      case TUTORIALSPOINT -> "TutorialsPoint";
      case BAELDUNG -> "Baeldung";
      case JENKOV -> "Jenkov";
      case MKYONG -> "Mkyong";
      case DIGITALOCEAN -> "DigitalOcean";
      case JAVATPOINT -> "JavaTpoint";
      case W3SCHOOLS -> "W3Schools";
      case VOGELLA -> "Vogella";
      case ZETCODE -> "ZetCode";
      case HOWTODOINJAVA -> "HowToDoInJava";
      case REFLECTORING -> "Reflectoring";
      case GEEKSFORGEEKS -> "GeeksforGeeks";
      case STACKABUSE -> "StackAbuse";
      case FREECODECAMP -> "FreeCodeCamp";
      case CODEJAVA -> "CodeJava";
      case JAVAGUIDES -> "JavaGuides";
      case DEV_JAVA -> "DevJava";
      case SPRING_GUIDES -> "SpringGuides";
      case MDN -> "Mdn";
    };
  }

  public static String aspectSuffix(M3TutorialReferenceCatalog.Aspect aspect) {
    return switch (Objects.requireNonNull(aspect, "aspect")) {
      case JAVA_CORE -> "JavaCore";
      case COLLECTIONS_ALGORITHMS -> "CollectionsAlgorithms";
      case CONCURRENCY -> "Concurrency";
      case IO_NETWORKING -> "IoNetworking";
      case PERSISTENCE_DATABASE -> "PersistenceDatabase";
      case SPRING_WEB_API -> "SpringWebApi";
      case SECURITY -> "Security";
      case TESTING_BUILD -> "TestingBuild";
      case DATA_SERIALIZATION -> "DataSerialization";
      case INTEGRATION_MESSAGING -> "IntegrationMessaging";
      case FRONTEND_WEB -> "FrontendWeb";
      case NATIVE_JNI -> "NativeJni";
      case ARCHITECTURE_PATTERNS -> "ArchitecturePatterns";
      case DEVOPS_CLOUD -> "DevopsCloud";
      case AI_LLM -> "AiLlm";
      case MAVEN_OPENREWRITE -> "MavenOpenrewrite";
    };
  }
}
