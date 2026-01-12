// GRIN Android Library Module
// Zero-dependency core library for rendering .grin files on Android

plugins {
    id("com.android.library")
    kotlin("android")
    id("de.mannodermaus.android-junit5")
    id("org.jetbrains.dokka")
    id("maven-publish")
    id("signing")
}

android {
    namespace = "io.grin.lib"
    compileSdk = 34

    defaultConfig {
        minSdk = 21  // Android 5.0 Lollipop - minimum supported
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        buildConfig = true
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                // Publish the Android release variant to Maven-compatible repositories.
                from(components["release"])
                groupId = project.group.toString()
                artifactId = "grin-android"
                version = project.version.toString()

                pom {
                    name.set("GRIN Android Library")
                    description.set("GRIN (Graphic Readdressable Indexed Nodes) Android/Kotlin implementation.")
                    url.set("https://github.com/grin-format/grin")

                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                            distribution.set("repo")
                        }
                    }

                    developers {
                        developer {
                            id.set("grin-contributors")
                            name.set("GRIN Project Contributors")
                            url.set("https://github.com/grin-format/grin")
                        }
                    }

                    scm {
                        connection.set("scm:git:https://github.com/grin-format/grin.git")
                        developerConnection.set("scm:git:ssh://git@github.com:grin-format/grin.git")
                        url.set("https://github.com/grin-format/grin")
                    }
                }
            }
        }

        repositories {
            maven {
                // Central Publisher Portal endpoint for Maven Central deployments.
                name = "central"
                url = uri("https://central.sonatype.com/api/v1/publisher")
                credentials {
                    username = findProperty("centralUsername") as String?
                        ?: System.getenv("CENTRAL_USERNAME")
                    password = findProperty("centralPassword") as String?
                        ?: System.getenv("CENTRAL_PASSWORD")
                }
            }
        }
    }
}

signing {
    // Only require signing when PGP credentials are provided.
    val signingKey = findProperty("signingKey") as String? ?: System.getenv("SIGNING_KEY")
    val signingPassword = findProperty("signingPassword") as String?
        ?: System.getenv("SIGNING_PASSWORD")
    if (!signingKey.isNullOrBlank() && !signingPassword.isNullOrBlank()) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications["release"])
    }
}

tasks.dokkaHtml {
    outputDirectory.set(rootProject.file("../docs/api/android"))
    dokkaSourceSets.configureEach {
        jdkVersion.set(8)
        reportUndocumented.set(false)
        skipDeprecated.set(false)
    }
}

dependencies {
    // Core AndroidX - minimal dependencies for stability
    implementation("androidx.annotation:annotation:1.7.1")
    
    // Unit testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.21")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.10.2")
    
    // Instrumented testing
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
