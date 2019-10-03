num_measurements <- 14
measurement_times <-
c(1, 6, 11, 16, 21, 26, 31, 41, 51, 61, 71, 81, 91, 101)
expression <-
c(0.05292949558, 0.03947862943, 0.0486713201, 0.02286390624, 0.04790153558, 0.09693864422,
0.04694826701, 0.09318333114, 0.0983600116, 0.08598421925, 0.0694546798, 0.08259599939, 0.1058589912,
0.06607350627)
measurement_sigma_absolute <- 0.1
measurement_sigma_relative <- 0.1
initial_condition_prior_sigma <- 2
asymptotic_normalized_state_prior_sigma <- 2
degradation_prior_mean <- -2
degradation_prior_sigma <- 1