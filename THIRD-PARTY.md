# Third-party notices

The editor is released under GPL-3.0-or-later. Its license does not replace the original licenses of third-party components.

## Direct runtime dependencies

| Component | Version | License | Original notices |
| --- | --- | --- | --- |
| PlantUML | 1.2026.9 | GPL-3.0-or-later, with separately licensed bundled components | [PlantUML notices](licenses/plantuml/) |
| RSyntaxTextArea | 3.6.0 | BSD-3-Clause | [Original license](licenses/rsyntaxtextarea/META-INF/LICENSE) |
| FlatLaf | 3.7.2 | Apache-2.0 | [Original license](licenses/flatlaf/META-INF/LICENSE) |

## Components embedded in PlantUML

The published PlantUML JAR is itself a bundled distribution. Excluding its Maven transitive dependencies does not remove libraries already embedded in that JAR.

- [Original bundled-license files](licenses/plantuml/META-INF/) include the JLaTeXMath license and linking exception, GPL-2.0 text, OpenPDF license statement, MPL-2.0, LGPL-2.1, Apache-2.0, and miscellaneous OpenPDF notices.
- [Original Java source notices](licenses/plantuml/SOURCE-NOTICES.txt) preserve 93 distinct source-header blocks from the exact PlantUML source artifact, with their original source paths. These cover PlantUML, Smetana/translated Graphviz code, JCCKit, ditaa, and other source-level components present in the upstream artifact.
- [Embedded Maven inventory](licenses/plantuml/BUNDLED-MAVEN-COMPONENTS.txt) records component coordinates found inside the actual PlantUML JAR. Their original POM metadata is retained under `licenses/plantuml/META-INF/maven/`. This includes JLaTeXMath and its Greek/Cyrillic fonts, Eclipse ELK, Xtext, OpenPDF, Guava and companion libraries, Brotli4j, Error Prone annotations, and J2ObjC annotations.
- [Font notices](licenses/plantuml/org/scilab/forge/jlatexmath/fonts/licences/) include SIL OFL 1.1, the Knuth notice, and the dsrom notice.
- [Native Graphviz license](licenses/graphviz/LICENSE.txt) is extracted directly from PlantUML's bundled Graphviz resource. Its [file inventory](licenses/graphviz/BUNDLED-FILES.tsv) records the unmodified native components included upstream.
- [EPL-1.0](licenses/EPL-1.0.txt) and [EPL-2.0](licenses/EPL-2.0.md) are also supplied for the relevant Eclipse/Graphviz components.

Files extracted from upstream distributions are preserved verbatim. The editor does not modify the bundled libraries or native Graphviz binaries.

## Source access

Matching published source artifacts:

- [PlantUML 1.2026.9 sources](https://repo.maven.apache.org/maven2/net/sourceforge/plantuml/plantuml/1.2026.9/plantuml-1.2026.9-sources.jar); [tagged repository](https://github.com/plantuml/plantuml/tree/v1.2026.9).
- [RSyntaxTextArea 3.6.0 sources](https://repo.maven.apache.org/maven2/com/fifesoft/rsyntaxtextarea/3.6.0/rsyntaxtextarea-3.6.0-sources.jar); [tagged repository](https://github.com/bobbylight/RSyntaxTextArea/tree/3.6.0).
- [FlatLaf 3.7.2 sources](https://repo.maven.apache.org/maven2/com/formdev/flatlaf/3.7.2/flatlaf-3.7.2-sources.jar); [tagged repository](https://github.com/JFormDesigner/FlatLaf/tree/3.7.2).
- [Graphviz distributions and build/source information](https://github.com/plantuml/graphviz-distributions).
- For embedded Maven artifacts, their exact coordinates are listed in the inventory. Matching source artifacts are available in Maven Central by those coordinates; the original POMs also identify their upstream projects.

## Test and build dependencies

JUnit 6.0.3 is used only for tests, not included in the runnable JAR. Its original EPL-2.0 license and notices are retained in [licenses/junit](licenses/junit/).

Apache Maven is a build prerequisite and is not included in the application. Its distribution license and notice are supplied in [licenses/build-tools](licenses/build-tools/). Java is supplied by the user and is not redistributed.

All runtime notices are included both in this repository and inside the application JAR under `META-INF/licenses/`.

