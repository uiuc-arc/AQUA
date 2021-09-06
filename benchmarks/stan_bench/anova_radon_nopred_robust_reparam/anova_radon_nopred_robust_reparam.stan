data {
  int<lower=0> J;
  int<lower=0> N;
  int<lower=1,upper=J> county[N];
  vector[N] y;
}
parameters {
  vector[J] a;
  real<lower=0,upper=100> sigma_y;
  real<lower=0, upper=10> robust_local_tau[N];
}

model {
  for (i in 1:N) {
    y[i] ~ normal(a[county[i]], sigma_y*inv(sqrt(robust_local_tau[i])));
  }
}
