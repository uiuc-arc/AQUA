data {
 int<lower = 0> N;
 vector[N] y;
}

parameters {
  vector[2] mu;
  real<lower=0,upper=5> sigma[2];
}

model {
 mu[1] ~ normal(2.75, 0.5);
 mu[2] ~ normal(-2.75, 0.5);
 for (n in 1:N)
   target += log_mix(0.3,
                     normal_lpdf(y[n] | mu[1], sigma[1]),
                     normal_lpdf(y[n] | mu[2], sigma[2]));
}
