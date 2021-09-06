data {
  int<lower=0> N;
  int<lower=0,upper=1> switched[N];
  vector[N] dist100;
  vector[N] educ4;
}
parameters {
  vector<lower=0,upper=1>[2] w;
}
model {
   w ~ uniform(0,1);
   for (i in 2:N)
          switched[i] ~ bernoulli_logit(fmax(dist100[i]*w[1],w[2]));
          
}
