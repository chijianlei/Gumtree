# GumTree

## Description

GumTree is a complete framework to deal with source code as trees and compute differences between them. It includes possibilities such as:
* converting a source file into a language-agnostic tree format
* export the produced trees in various formats
* compute the differences between the trees
* export these differences in various formats
* visualize these differences graphically

Compared to classical code differencing tools, it has two important particularities:
* it works on a tree structure rather than a text structure,
* it can detect moved or renamed elements in addition of deleted and inserted elements.

## Prerequisites

GumTree requires Java 1.9 to run.
You have to install srcML if you want to diff C++ and C# code
You have to install cgum if you want to diff C code
You have to install pythonparser to diff Python code
All external tools have to be available in your system's path.

## Documentation

To use GumTree, you can start by consulting the [Getting Started](https://github.com/GumTreeDiff/gumtree/wiki/Getting-Started) page from our [wiki](https://github.com/GumTreeDiff/gumtree/wiki).

## Supported languages

We already deal with a wide range of languages: C, Java, JavaScript, Python, R, Ruby. Click [here](https://github.com/GumTreeDiff/gumtree/wiki/Languages) for more details about the language we support.

## Usage and Example

### From a release：
You can download a release of GumTree directly on GitHub. 
Unzip the file and you will find gumtree's binaries in the bin folder.

### From the sources
You can build GumTree with the following commands:

```
git clone https://github.com/chijianlei/Gumtree.git
cd gumtree
./gradlew build -x test
```
You will have a zip distribution of GumTree in the `dist/build/distributions` folder. The gumtree binary is located in the bin folder contained in this archive.

Windows notes
Instead of `./gradlew build -x test`, run `gradlew.bat build -x test`

### From maven
GumTree's Maven modules are available here: http://mvnrepository.com/artifact/com.github.gumtreediff.

### From Docker
You can use our docker image: https://github.com/GumTreeDiff/gumtree/tree/develop/docker. Follow our instructions.

### For Examples： 

#### WebDiff:
```
gumtree webdiff PATH1 PATH2
```
Description: webdiff starts a webserver that displays a diff between two files or two directories.
It will perform a diff and will display it in a browser.

#### SwingDiff
```
gumtree swingdiff PATH1 PATH2
```
Description: swingdiff displays the diff between two files using the Swing java UI toolkit.

#### Textdiff
```
gumtree textdiff PATH1 PATH2
```
Description: diff outputs the diff between two files in a textual format.

#### Dotdiff
```
gumtree dotdiff PATH1 PATH2
```
Description: diff outputs the diff between two files in a dot format.

#### Xmldiff
```
gumtree axmldiff PATH1 PATH2
```
Description: diff outputs the diff between two files in a xml format.

#### Cluster
```
gumtree cluster PATH1 PATH2
```
Description: Extract transformation action clusters.

#### Parse
```
gumtree parse PATH
```
Description: parse outputs the AST contained in the given file.

## Citing GumTree

We are researchers, therefore if you use GumTree in an academic work we would be really glad if you cite our seminal paper using the following bibtex:

```
@inproceedings{DBLP:conf/kbse/FalleriMBMM14,
  author    = {Jean{-}R{\'{e}}my Falleri and
               Flor{\'{e}}al Morandat and
               Xavier Blanc and
               Matias Martinez and
               Martin Monperrus},
  title     = {Fine-grained and accurate source code differencing},
  booktitle = {{ACM/IEEE} International Conference on Automated Software Engineering,
               {ASE} '14, Vasteras, Sweden - September 15 - 19, 2014},
  pages     = {313--324},
  year      = {2014},
  url       = {http://doi.acm.org/10.1145/2642937.2642982},
  doi       = {10.1145/2642937.2642982}
}
```
