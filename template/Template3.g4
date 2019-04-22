grammar Template3;

WS : [ \n\t\r]+ -> channel(HIDDEN) ;

// primitives
ID: [a-zA-Z]+[a-zA-Z0-9_]*;
STRING : '"' ~["]* '"' ;
INT : '-'?[0-9]+;
DOUBLE : '-'? (([0-9]? '.' [0-9]+) | ([1-9][0-9]* '.' [0-9]*)) (('E'|'e') '-'? [0-9]+)?;
INTEGERTYPE: 'int';
FLOATTYPE: 'float';
//holes
DISTHOLE: 'DIST';
CONSTHOLE: 'CONST';
DISTXHOLE: 'DISTX';

primitive: INTEGERTYPE | FLOATTYPE;
number: INT | DOUBLE;
limits : '<' ( 'lower' '=' expr ',' 'upper' '=' expr | 'lower' '=' expr | 'upper' '=' expr ) '>' ;
marker : 'start' | 'end';
//dotted_name: ID ('.' ID)*;
annotation_type: 'blk' | 'type' | 'dimension' | 'limits' | 'observe' | 'prior';
annotation_value: ID | dims | limits | marker;
annotation: '@' annotation_type ('.' annotation_value)?; // annotation for block, types, prior and observes

// types and dimensions
dims: expr (',' expr)*;
vectorDIMS : '<' dims '>';
dtype : primitive '[' dims ']'?;

// data
array : '[' expr ( ',' expr )* ']' | '[' array (',' array)* ']' | '[' vector (',' vector)* ']' | '[' ']';
vector : '<' expr ( ',' expr )* '>' | '<' vector (',' vector)* '>' | '<' array (',' array)* '>' | '<' '>';

// statements
data: ID ':'  ( dtype | expr | array | vector ) ;
//prior: expr ':=' expr  ;
function_call : (( ID | DISTHOLE ) '(' (expr (',' expr)*)? ')' ) | DISTXHOLE;
for_loop: 'for' '(' ID 'in' expr ':' expr ')' block;
if_stmt: 'if' '(' expr ')' block ('else' 'if' '(' expr ')' block )* ('else' block)? ;
assign: expr '=' expr ;
decl: dtype ('[' dims ']')? ID ('[' dims ']')? | function_decl;

statement: assign | for_loop | if_stmt | decl | function_call | annotation;
block: '{' statement* '}' | statement ;

// function definitions
fparam : return_or_param_type ID;
fparams : fparam (',' fparam)*;

return_or_param_type: dtype | 'void' | ( dtype ('[' ']')+ ) ;
function_decl: return_or_param_type ID '(' fparams? ')' block;

expr :
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
    | number            #val
    | ID                #ref
    | ID '[' dims ']'   #array_access
    | STRING            #string
    | CONSTHOLE         #consthole
    ;

query: ('posterior' | 'expectation') '(' ID ')' ;
template: (data | statement | query)* ;