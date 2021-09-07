// geometric lag time-series (Koyck 1951)
//
// http://en.wikipedia.org/wiki/Distributed_lag

data {
  int<lower=0> T;   // number of time points
  real y[T];        // output at time t
  real x[T];        // predictor for time t
}
parameters {
  real<lower=-5,upper=5> alpha;                       // intercept
  real<lower=-5,upper=5> beta;                        // slope
  real <lower=0, upper=1> lambda;   // lag
}
model {
  for (t in 2:T)
    y[t] ~ normal(alpha +  beta * x[t] +  lambda * y[t-1],
                  0.5);
}

