data {
    int N;
    vector[N] y;

}
parameters {
    vector<lower=-10, upper=10>[2] mu;
}
model {
    mu ~ uniform(-10.0,10.0);
    y ~ normal((mu[1] + mu[2]), 1.0);

}
