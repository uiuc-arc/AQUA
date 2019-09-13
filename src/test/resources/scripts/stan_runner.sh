#!/usr/bin/env bash
# first argument is the directory where everthing should be setup
rp=`realpath $0`
bp=`dirname $rp`
echo $bp
cd $bp

file=$1
data=$2
basefile=`basename $file`
basedata=`basename $data`
basefilename=`echo $basefile | cut -d"." -f1`
echo $pwd
if [ ! -d "cmdstan-2.19.1" ]; then
    wget https://github.com/stan-dev/cmdstan/releases/download/v2.19.1/cmdstan-2.19.1.tar.gz
    tar -xf cmdstan-2.19.1.tar.gz
    rm -f cmdstan-2.19.1.tar.gz
fi

cd cmdstan-2.19.1
make build
if [ ! -d "$basefilename" ]; then
    mkdir $basefilename
fi
echo $pwd

cp $1 $2 $basefilename/


echo "making..."
#rm -f stan_file/stan2237
make $basefilename/$basefilename
cd ./$basefilename/

if [ ! -z "$3" ]; then
./$basefilename sample algorithm=$3 num_samples=1000 num_warmup=200 data file=$basedata > stanout 2>&1
else
./$basefilename sample num_samples=1000 num_warmup=200 data file=$basedata > stanout 2>&1
fi


