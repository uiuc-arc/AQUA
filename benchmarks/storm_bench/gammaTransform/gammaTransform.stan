parameters {
    real<lower=0> X;
}
model {
    real Y;
    real Z;
    X ~ gamma(3,1);
    if (X < 1) {
        Y = -1/exp(X*X);
    }
    else {
        Y = 1/log(X);
    }
    Z = -Y*Y*Y+Y*Y+6*Y;
}
