#!/usr/bin/env bash
# ./build.sh -l [Python2|Java] -o [output_dir] -p [package] -g [grammarfile]
# ./build.sh -c [dir] #cleans all temp files

print_usage(){
    echo "Usage: ./build.sh -l  [Python2|Java] -o [output_dir] -p [package] -g [grammarfile] ";
}
while getopts "p:l:o:g:c:" opt; do
    case ${opt} in
	p) package="-package $OPTARG";;
	l) language="-Dlanguage=$OPTARG";;
	o) output="-o $OPTARG";;
	g) grammarfile=$OPTARG;;
	c) d=$OPTARG;
	   (cd $d; rm -f *.interp *.tokens *.py *.java);
	   echo "Cleaned all files";
	   exit 0;;
	\?) print_usage; exit 1;;
    esac
done

if [ -z $grammarfile ]; then
    echo "Grammar file not specified"
    print_usage
    exit 1
elif [ -z $language ]; then
    echo "Language not specified"
    print_usage
    exit 1
fi

antlr4='java -Xmx500M -cp ".:./antlr-4.7.1-complete.jar:$CLASSPATH" org.antlr.v4.Tool'

$antlr4 $package $language $output -Xexact-output-dir  -visitor $grammarfile

