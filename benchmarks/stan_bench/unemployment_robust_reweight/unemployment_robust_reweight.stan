data {
  int<lower=0> N; 
  vector[N] y;
  vector[N] y_lag;
}
parameters {
  vector[2] beta;
  real<lower=0> sigma;
  real<lower=0, upper=1> robust_weight[N];
} 
model {
  for ( i in 1:N) {
	   target += normal_lpdf(y[i]|beta[1] + beta[2] * y_lag[i],sigma)*robust_weight[i];
  }
}
