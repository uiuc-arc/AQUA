grammar Data;

WS : [ \n\t\r]+ -> channel(HIDDEN) ;
ID : [a-zA-Z]+[a-zA-Z0-9_]* ;
INT : [-]?[0-9]+ ;
//INT : [-]?[0-9]+ (('e' | 'E')[0-9]+)?;
DOUBLE :  [-]?(([0-9]? '.' [0-9]+) | ([1-9][0-9]* '.' [0-9]+)) (('e' | 'E') [-]? [0-9]+)? ;

primitive : INT ('L'|'l')? | DOUBLE ;
dim : '.Dim' '=' array;
structure: 'structure' '('  array  ',' dim ')';
array : 'c' '(' primitive (',' primitive)* ')';

dt : primitive | array | structure;

assign : ID '<-'  dt;
datafile : assign*;
