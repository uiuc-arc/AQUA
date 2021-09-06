data {
    int N;
    vector[N] y;

}
parameters {
    vector<lower=-2, upper=2>[3] mu;
}
model {
    mu ~ normal(0.0,5.0);
    y ~ normal((3.0*mu[1]*mu[2])-mu[3], 1.0);

}
