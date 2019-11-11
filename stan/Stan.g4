grammar Stan;

PRIMITIVE: 'int' | 'real' ;
COMPLEX : 'vector' | 'row_vector' | 'matrix' | 'unit_vector' | 'simplex' | 'ordered' | 'positive_ordered' | 'cholesky_factor_corr' | 'cholesky_factor_cov' | 'corr_matrix' | 'cov_matrix';


WS : [ \n\t\r]+ -> channel(HIDDEN) ;
ID : [a-zA-Z]+[a-zA-Z0-9_]* ;
NO_OP : ';' ;
OP : '+' | '-' | '/' | '*' | '^' ;
INT : [0-9]+ ('E'[0-9]+)?;
//tofix: + sign before integers?
DOUBLE :  (([0-9]? '.' [0-9]+) | ([1-9][0-9]* '.' [0-9]+)) ('E'[0-9]+)? ;
COMMENT : '/' '/' .*? '\n' -> channel(HIDDEN);

STRING : '"' ~["]* '"' ;
COMP_OP : '==' | '<' | '>' | '!=' | '&&' | '||' | '<=' | '>=' ;

arrays : '{' (INT|DOUBLE) (',' (INT|DOUBLE))* '}' ;
type : PRIMITIVE | COMPLEX ;
inbuilt : 'to_vector' | 'block' | 'normal_cdf_log' | 'diag_matrix' | 'increment_log_prob' | 'log_sum_exp' | 'log' | 'gamma_p' | 'rep_vector' | 'rep_matrix' | 'rep_array' | ID;
dim : INT | ID | expression | ;
dims : '[' dim (',' dim)* ']' ;
limits : '<' ( 'lower' '=' expression ',' 'upper' '=' expression | 'lower' '=' expression | 'upper' '=' expression ) '>' ;
//decl : ( (Type ID) | (Type ID '[' ID ']') | Type limits? dims ID ) ';' ;
decl: type (limits)? dims? ID dims? ';' ;
print_stmt: 'print' '(' expression  (',' expression)* ')' ';' ;
function_call: inbuilt '(' (expression (',' expression )*)? ')' ;
//transpose_exp : expression '\'' ;

function_call_stmt: function_call ';' ;
assign_stmt : expression ('<-' | '=') expression ';' ;
array_access : ID dims ;
//comp_expression : expression COMP_OP expression ;
block : '{' statement* '}' | statement ;
if_stmt : 'if' '(' expression ')' block ( 'else' block )? ;
range_exp: expression ':' expression ;
for_loop_stmt : 'for' '(' ID 'in' range_exp ')' block ;
target_stmt : 'target' '+=' expression ';'  ;

expression :  expression '\''               #transpose
            | expression '^' expression     #exponop
            | expression '/' expression     #divop
            | expression '*' expression     #mulop
            | expression '+'  expression    #addop
            | expression '-' expression     #minusop
            | expression '==' expression    #eq
            | expression '<' expression     #lt
            | expression '>' expression     #gt
            | expression '!=' expression    #ne
            | expression '&&' expression    #and
            | expression '||' expression    #or
            | expression '<=' expression    #le
            | expression '>=' expression    #ge
            | expression '?' expression ':' expression   #ternary_if
            | (array_access | ID | INT | DOUBLE) '|' expression (',' expression )*   #condition
            | arrays              #array_decl
            | '-' expression    #unary
            | '(' expression ')' #brackets
            | function_call     #function
            | array_access      #array
            | STRING		#string
            | INT               #integer
            | DOUBLE            #double
            | ID                #id_access
            ;

//distributions : 'normal' | 'cauchy' | 'student_t' | 'double_exponential' | 'logistic' | 'gumbel' | 'lognormal' | 'chi_square' | 'inv_chi_square' | 'exponential' | 'gamma' | 'inv_gamma' | 'weibull' | 'beta' | 'uniform' | 'bernoulli_logit' | 'binomial' | 'beta_binomial' | 'neg_binomial' | 'poisson' ;
distribution_exp : ID '(' expression (',' expression )* ')' ;
sample : expression '~' distribution_exp ';' ;
return_or_param_type: type | 'void' | ( type ('[' ']')+ ) ;
params : return_or_param_type ID  (',' return_or_param_type ID)* ;
function_decl: return_or_param_type ID '(' params? ')' block;
return_stmt : 'return' expression ';' ;
statement : NO_OP | sample | decl | print_stmt | function_call_stmt | assign_stmt | if_stmt | for_loop_stmt | return_stmt | target_stmt;

datablk : 'data'  '{' decl* '}' ;
paramblk : 'parameters' '{' decl* '}' ;
modelblk : 'model' block ;
transformed_param_blk : 'transformed parameters' block;
transformed_data_blk : 'transformed data' block;
generated_quantities_blk : 'generated quantities' block;
functions_blk : 'functions' '{' function_decl* '}';

program : (datablk | paramblk | modelblk | transformed_param_blk | transformed_data_blk | generated_quantities_blk | functions_blk)+;

