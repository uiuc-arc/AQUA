// Fit the hyperparameters of a latent-variable Gaussian process with an
// exponentiated quadratic kernel and a Bernoulli likelihood

data {
  int<lower=1> N;
  vector[N] x;
  int<lower=0, upper=1> y[N];
}
parameters {
  real w;
  real b;
}
model {
  y ~ bernoulli_logit(w * x + b);
}
