data {
  int<lower=0> N; 
  vector[N] y;
  vector[N] y_lag;
}
parameters {
  vector[2] beta;
  real<lower=0> sigma;
} 
model {
  beta ~ normal(1,1);
  y ~ normal(beta[1] + beta[2] * y_lag,sigma);
}
