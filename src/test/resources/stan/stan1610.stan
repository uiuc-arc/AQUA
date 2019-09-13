data {
  int<lower=2> K;                
  int<lower=2> V;                
  int<lower=2> U;                
  int<lower=2> I;                
  int<lower=1> N;                
  int<lower=1,upper=V> word[N];  
  int<lower=1,upper=I> item[N];  
  int<lower=1,upper=U> user[N];  
  vector<lower=0, upper=1>[K] alpha_user; 
  vector<lower=0, upper=1>[K] alpha_item; 
  vector<lower=0, upper=1>[V] beta;       
}
parameters {
  simplex[K] item_topics[I];    
  simplex[K] user_topics[U];    
  simplex[V] word_topics[K];    
}
model {
  for (i in 1:I)  
    item_topics[i] ~ dirichlet(alpha_item);  
  for (u in 1:U)  
    user_topics[u] ~ dirichlet(alpha_user);  
  for (k in 1:K)  
    word_topics[k] ~ dirichlet(beta);     
  
  for (n in 1:N) {
    real gamma[K];
    
    for (k in 1:K){ 
      
      gamma[k] <- log(item_topics[item[n], k] + user_topics[user[n], k]) + log(word_topics[k, word[n]]);
    }
    increment_log_prob(log_sum_exp(gamma));  
  }
}