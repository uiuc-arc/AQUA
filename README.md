# AQUA

AQUA is a tool for probabilistic inference that operates on probabilistic programs with continuous posterior distributions. 
AQUA approximates programs via an efficient quantization of the contin- uous distributions.


## Installation

Prerequisites:

* Java
* Maven (e.g. `sudo apt -y update; sudo apt install maven`)

Install dependencies and build package:

    mvn package -DskipTests=true

It should print `BUILD SUCCESS`.


## Usage

AQUA can take as input either (a) a template file, or (b) a directory containing stan file and data. 

---------------------------

(a) Run AQUA on a template file: 
    
    mvn exec:java -Dexec.mainClass="grammar.analyses.AnalysisRunner" -Dexec.args="<path_to_input_template_file>"
    
E.g. 

    mvn exec:java -Dexec.mainClass="grammar.analyses.AnalysisRunner" -Dexec.args="./benchmarks/psi/three_coin_flip/three_coin_flip.template"
    
----------------------------

(b) Run AQUA on a stan file. The `path_to_input_dir` must contain a stan file (dir_name.stan) and a data file (dir_name.data.R) with the same name as the directory.
    
    mvn exec:java -Dexec.mainClass="grammar.analyses.AnalysisRunner" -Dexec.args="<path_to_input_dir>"
    
E.g., the directory `./benchmarks/all/anova_radon_nopred` contains `anova_radon_nopred.stan` and `anova_radon_nopred.data.R`.

    mvn exec:java -Dexec.mainClass="grammar.analyses.AnalysisRunner" -Dexec.args="./benchmarks/all/anova_radon_nopred"

