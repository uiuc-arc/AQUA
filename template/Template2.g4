grammar Template2;

FUNCTION: 'log' | 'sqrt' | 'increment_log_prob' | 'mean' | 'pow' | 'exp' | 'inv_cloglog' | 'inv_logit' | 'logit' | 'col' | 'int_step' | 'Phi' | 'log2' | 'exp2' | 'inv' | 'inv_sqrt'| 'inv_square'|  'square' |  'to_vector' | 'block' | 'diag_matrix' | 'log_sum_exp' | 'gamma_p' | 'rep_vector' | 'rep_matrix' | 'rep_array' |  'abs' |  'min' | 'max' | 'e' |  'sqrt2' |  'log10' |  'not_a_number' |  'positive_infinity' | 'negative_infinity' | 'machine_precision' | 'get_lp' | 'step' | 'is_inf' | 'is_nan' | 'fabs' | 'fdim' | 'fmin' | 'fmax' | 'fmod' | 'floor' | 'ceil' | 'round' | 'trunc' | 'cbrt' |   'hypot' | 'cos' | 'sin' | 'tan' | 'acos' | 'asin' | 'atan' | 'atan2' | 'cosh' | 'sinh' | 'tanh' | 'acosh' | 'asinh' | 'atanh' | 'erf' | 'erfc' | 'inv_Phi' | 'Phi_approx' | 'binary_log_loss' | 'owens_t' | 'inc_beta' | 'lbeta' |'tgamma' | 'lgamma' | 'digamma' | 'trigamma' | 'lmgamma' |  'gamma_q' | 'binomial_coefficient_log' | 'softmax' | 'sd' | 'log1m' | 'determinant' | 'log1p' | 'normal_log' | 'cauchy_log' | 'binomial_log' | 'lognormal_lpdf' | 'pareto_lpdf' |  'normal_cdf_log' |  'pi' | 'sum' | 'diag_pre_multiply' | 'inverse' | 'normal_cdf' | 'dot_self' | 'bernoulli_logit_lpmf' | 'if_else' | 'normal_lpdf' | 'append_row' | 'print' | 'lognormal_rng' | 'poisson_rng' | 'categorical_rng' | 'reject' | 'multiply_lower_tri_self_transpose' | 'multinomial_rng' | 'dirichlet_rng' | 'binomial_rng' | 'return' | 'rep_row_vector' | 'cov_exp_quad' | 'cholesky_decompose' | 'size' | 'rows' |'normal_rng' | 'mdivide_left_tri_low' | 'bernoulli_logit_rng' | 'multi_normal_rng' | 'log_mix' | 'poisson_log_lpmf' | 'binomial_lpmf' | 'bernoulli_lpmf' | 'binomial_logit_lpmf' | 'prod' | 'bernoulli_rng' | 'cumulative_sum' | 'categorical_log';
INTEGERTYPE: 'int';
FLOATTYPE: 'float';
INT :[0-9]+;
DOUBLE : ([0-9]? '.' [0-9]+) | ([1-9][0-9]* '.' [0-9]+) | [0-9]+'E'[0-9]+ ;
DISTHOLE: 'DIST';
CONSTHOLE: 'CONST';
DISTXHOLE: 'DISTX';
COMPLEX : 'vector' | 'row_vector' | 'matrix' | 'unit_vector' | 'simplex' | 'ordered' | 'positive_ordered' | 'cholesky_factor_corr' | 'cholesky_factor_cov' | 'corr_matrix' | 'cov_matrix';
//AOP: '+' | '-' | '*' | '^' | '/' ;
//BOP: '>=' | '<=' | '<' | '>' | '==' | '&&' | '!=';
WS : [ \n\t\r]+ -> channel(HIDDEN) ;
ID: [a-zA-Z]+[a-zA-Z0-9_]*;
STRING : '"' ~["]* '"' ;

primitive: INTEGERTYPE | FLOATTYPE;
number: INT | DOUBLE | CONSTHOLE vectorDIMS?;
dtype: primitive | (primitive '[' dims ']') | COMPLEX;
array : '[' expr ( ',' expr )* ']' | '[' array (',' array)* ']' | '[' vector (',' vector)* ']' | '[' ']';
vector : '<' expr ( ',' expr )* '>' | '<' vector (',' vector)* '>' | '<' array (',' array)* '>' | '<' '>';

dims: expr (',' expr)*;
vectorDIMS : '<' dims '>';

decl: dtype (limits)?   ('[' dims ']')?  ID  ('[' dims ']')? ;
limits : '<' ( 'lower' '=' expr ',' 'upper' '=' expr | 'lower' '=' expr | 'upper' '=' expr ) '>' ;

data: (ID ':' dtype | ID ':' expr | ID ':' array | ID ':' distexpr | ID ':' vector) ('[' dims ']')?;
prior: expr ':=' distexpr limits? ;

param: CONSTHOLE vectorDIMS? | expr;
params: param (',' param )*;
distexpr: (DISTHOLE | ID) vectorDIMS? '(' params ')' ('[' dims ']')? | DISTXHOLE vectorDIMS? ('[' dims ']')?;
loopcomp: '[' distexpr 'for' ID 'in' ID ']' ;
for_loop: 'for' '(' ID 'in' expr ':' expr ')' block;
if_stmt: 'if' '(' expr ')' block else_blk? ;
else_blk : 'else' block;
function_call: FUNCTION '(' params? ')' ;

fparam : return_or_param_type ID;
fparams : fparam (',' fparam)*;

return_or_param_type: dtype | 'void' | ( dtype ('[' ']')+ ) ;
function_decl: return_or_param_type ID '(' fparams? ')' block;

block: '{' statement* '}' | statement ;
transformedparam: 'transformedparam' block ;
transformeddata: 'transformeddata' block;
generatedquantities: 'generatedquantities' block;
functions: 'functions' '{' function_decl* '}';

expr:
     expr '\''         #transpose
    | expr '?' expr ':' expr #ternary
    // | expr AOP expr     #arith
    | expr '^' expr     #exponop
    | expr '/' expr     #divop
    | expr '*' expr     #mulop
    | expr '+'  expr    #addop
    | expr '-' expr     #minusop
    | expr './' expr    #vecdivop
    | expr '.*' expr    #vecmulop
    | expr '<' expr     #lt
    | expr '<=' expr    #leq
    | expr '>' expr     #gt
    | expr '>=' expr    #geq
    | expr '!=' expr    #ne
    | expr '==' expr    #eq
    | expr '&&' expr    #and
    | expr '[' expr ':' expr ']' #subset
    | function_call     #function
    | '-' expr          #unary
    | '(' expr ')'      #brackets
    |  number            #val
    | ID                #ref
    | ID '[' dims ']'   #array_access
    | STRING            #string
    ;

assign: ID '=' distexpr | ID '=' expr | expr '=' expr | expr '=' distexpr;
observe: 'observe' '(' ( expr | distexpr | loopcomp ) ',' expr ')' ;
statement: assign | for_loop | observe | if_stmt | prior | decl | function_call;

query: ('posterior' | 'expectation') '(' ID ')' ;

template : functions? data* transformeddata? statement* transformedparam? statement* generatedquantities? query*;