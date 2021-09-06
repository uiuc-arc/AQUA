data {
  int<lower=0> N; 
  vector[N] y;
}
parameters {
  vector<lower=21,upper=32>[1] beta;
  real<lower=0,upper=20> sigma;
} 
model {
  y ~ normal(beta[1],sigma);
}
