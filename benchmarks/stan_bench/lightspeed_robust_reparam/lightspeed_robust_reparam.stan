data {
  int<lower=0> N; 
  vector[N] y;
}
parameters {
  vector[1] beta;
  real<lower=0> sigma;
  real<lower=0, upper=10> robust_local_tau[N];
} 
model {
  for (i in 1:N) {
    y[i] ~ normal(beta[1],sigma*inv(sqrt(robust_local_tau[i])));
  }
}
