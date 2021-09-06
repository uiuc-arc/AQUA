# AQUA

AQUA is a tool for probabilistic inference that operates on probabilistic programs with continuous posterior distributions. It approximates programs via an efficient quantization of the continuous distributions. AQUA represents the distributions of random variables using quantized value intervals (Interval Cube) and corresponding probability densities (Density Cube). It can use an adaptive algorithm for selecting the size and the granularity of the Interval and Density Cubes.

The paper describing the methodology behind AQUA: 

* [AQUA: Automated Quantized Inference for Probabilistic Programs](https://misailo.cs.illinois.edu/papers/aqua-atva21.pdf), Zixin Huang, Saikat Dutta, Sasa Misailovic, 19th International Symposium on Automated Technology for Verification and Analysis (ATVA 2021), Gold Coast, Australia, October 2021


## Installation

Prerequisites:

* Java
* Maven (e.g. `sudo apt -y update; sudo apt install maven`)

Install dependencies and build package:

    mvn package -DskipTests=true

It should print `BUILD SUCCESS`.


## Usage

AQUA can take as input either  (a) a program in [Storm IR](https://misailo.cs.illinois.edu/papers/storm-fse19.pdf) (`<prog_name>.template`), see [examples](); or
(b) a directory `<prog_name>/` containing Stan file (`<prog_name>.stan`) and data (`<prog_name>.data.R`). 

#### (a) Run AQUA on a Storm IR file: 
    
    mvn exec:java -Dexec.mainClass="grammar.analyses.AnalysisRunner" -Dexec.args="<path_to_input_template_file>"
    
E.g. 

    mvn exec:java -Dexec.mainClass="grammar.analyses.AnalysisRunner" -Dexec.args="./benchmarks/storm_bench/three_coin_flip/three_coin_flip.template"
    

#### (b) Run AQUA on a Stan file. 
The `path_to_input_dir` must contain a stan file (`<prog_name>.stan`) and a data file (`<prog_name>.data.R`) with the same name as the directory.
    
    mvn exec:java -Dexec.mainClass="grammar.analyses.AnalysisRunner" -Dexec.args="<path_to_input_dir>"
    
E.g., the directory `./benchmarks/stan_bench/anova_radon_nopred` contains `anova_radon_nopred.stan` and `anova_radon_nopred.data.R`.

    mvn exec:java -Dexec.mainClass="grammar.analyses.AnalysisRunner" -Dexec.args="./benchmarks/stan_bench/anova_radon_nopred"



## Outputs

For each parameter, there will be an output `analysis_<param>.txt` file storing the quantized posterior. It is under the same directory as the input `<prog_name>.template` or `<prog_name>.stan` file.

E.g. after analyzing `three_coin_flip.template`, AQUA will output a file `./benchmarks/storm_bench/three_coin_flip/three_coin_flip/analysis_A.txt`, with the content:

     {
      "filefrom": "dl4j",
      "ordering": "c",
      "shape":    [2, 2],
      "data":
             [[                      0, 1.000000000000000000E0 ], 
              [2.500000000000000000E-1, 7.500000000000000000E-1]]
    }

where the first row (`[0, 1]`) stores the Interval Cube (for values of the random variable A), and the second row (`[2.5E-1, 7.5E-1]`) stores the Density Cube (for the correponding probability).


## Citation

To cite AQUA, please use

    @article{huangaqua,
      title={AQUA: Automated Quantized Inference for Probabilistic Programs},
      author={Huang, Zixin and Dutta, Saikat and Misailovic, Sasa}
    }
