data {
 int<lower = 0> N;
 vector[N] y;
}

parameters {
    vector<lower=-5,upper=5>[1] mu;
}

model {
target += log_mix(0.8,normal_lpdf(mu[1]| 4, 0.5), normal_lpdf(mu[1]| -4, 0.5));
 for (n in 1:N)
    target += student_t_lpdf(y[n] | 5,  mu[1], 2);;
}
