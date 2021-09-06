data {
  int<lower=0> N; 
  int y[N];
  vector[N] x1;
}
parameters {
  real w1;
  real w2;
} 
model {
  y ~ bernoulli_logit(w1*x1 + w2);
}

