


functions {
  matrix L_cov_exp_quad_ARD(vector[] x,
                            real alpha,
                            vector rho,
                            real delta) {
    int N;
    matrix[size(x), size(x)] K;
    real neg_half;
    real sq_alpha;
    N = size(x);
    neg_half = -0.5;
    sq_alpha = square(alpha);
    for (i in 1:(size(x)-1)) {
      K[i, i] = sq_alpha + delta;
      for (j in (i + 1):N) {
        K[i, j] = sq_alpha * exp(neg_half *
                                 dot_self((x[i] - x[j]) ./ rho));
        K[j, i] = K[i, j];
      }
    }
    K[size(x), size(x)] = sq_alpha + delta;
    return cholesky_decompose(K);
  }
}
data {
  int<lower=1> N;
  int<lower=1> D;
  vector[D] x[N];
  vector[N] y;
}
transformed data {
  real delta;
  delta = 0.000000001;
}
parameters {
  vector<lower=0>[D] rho;
  real<lower=0> alpha;
  real<lower=0> sigma;
  vector[N] eta;
}
model {
  vector[N] f;
  matrix[N, N] L_K;

  L_K = L_cov_exp_quad_ARD(x, alpha, rho, delta);
  f = L_K * eta;

  rho ~ inv_gamma(5, 5);
  alpha ~ normal(0, 1);
  sigma ~ normal(0, 1);
  eta ~ normal(0, 1);

  y ~ normal(f, sigma);
}