grammar Template3;

primitive returns [AST.Primitive value]
:
INTEGERTYPE {$value=AST.Primitive.INTEGER;}
| FLOATTYPE {$value=AST.Primitive.FLOAT;};


number returns [AST.Number value]
: i=INT {$value = new AST.Integer($i.getText());}
| d=DOUBLE {$value = new AST.Double($d.getText());}
;

limits returns [AST.Limits value]
@init {$value = new AST.Limits();}
 : '<' ( 'lower' '=' expr {$value.lower=$expr.value;}',' 'upper' '=' expr {$value.upper=$expr.value;}
 | 'lower' '=' expr {$value.lower=$expr.value;}
 | 'upper' '=' expr {$value.upper=$expr.value;} ) '>' ;

marker returns [AST.Marker value]
: 'start' {$value=AST.Marker.Start;}
| 'end' {$value=AST.Marker.End;};


annotation_type returns [AST.AnnotationType value]
: 'blk' {$value = AST.AnnotationType.Blk;}
| 'type' {$value = AST.AnnotationType.Type;}
| 'dimension' {$value = AST.AnnotationType.Dimension;}
| 'limits' {$value = AST.AnnotationType.Limits;}
| 'observe' {$value = AST.AnnotationType.Observe;}
| 'prior' {$value = AST.AnnotationType.Prior;}
;

annotation_value returns [AST.ASTNode value]
: ID {$value = new AST.Id($ID.getText());}
| dims {$value = $dims.value;}
| limits {$value = $limits.value;}
| marker {$value = new AST.MarkerWrapper($marker.value);}
;

// annotation for block, types, prior and observes
annotation returns [AST.Annotation value]
@init {$value = new AST.Annotation();}
: '@' at=annotation_type {$value.annotationType = $at.value;}
( av=annotation_value {$value.annotationValue = $av.value;})?;


//// types and dimensions
dims returns [AST.Dims value]
@init {$value = new AST.Dims();}
: e1=expr {$value.dims.add($e1.value);} (',' e2=expr {$value.dims.add($e2.value);})*;
//vectorDIMS : '<' dims '>';

dtype returns [AST.Dtype value]
@init {$value = new AST.Dtype();}
 : p=primitive {$value.primitive = $p.value;} '[' dims {$value.dims=$dims.value;} ']'? ;
//
//// data
array returns [AST.Array value]
@init {$value = new AST.Array();}
 :
 '[' expr {$value.expressions = new ArrayList<>(); $value.expressions.add($expr.value);} ( ',' expr {$value.expressions.add($expr.value);})* ']'
 | '[' array {$value.arrays = new ArrayList<>(); $value.arrays.add($array.value);}(',' array {$value.arrays.add($array.value);})* ']'
 | '[' vector {$value.vectors = new ArrayList<>(); $value.vectors.add($vector.value);} (',' vector {$value.vectors.add($vector.value);})* ']'
 | '[' ']';
vector returns [AST.Vector value]

 @init {$value = new AST.Vector();}
  :
  '<' expr {$value.expressions = new ArrayList<>(); $value.expressions.add($expr.value);} ( ',' expr {$value.expressions.add($expr.value);})* '>'
  |  '<' array {$value.arrays = new ArrayList<>(); $value.arrays.add($array.value);} (',' array {$value.arrays.add($array.value);})* '>'
  | '<' vector {$value.vectors = new ArrayList<>(); $value.vectors.add($vector.value);} (',' vector {$value.vectors.add($vector.value);})* '>'
  | '<' '>';

//
//// statements
data returns [AST.Data value]
@init {$value = new AST.Data();}
: ID {$value.id = new AST.Id($ID.getText());} ':'
( dtype {$value.dtype = $dtype.value;}
| expr  {$value.expression = $expr.value;}
| array {$value.array = $array.value;}
| vector {$value.vector = $vector.value;}
) ;

function_call returns [AST.FunctionCall value]
@init {$value = new AST.FunctionCall();}
: (( ID {$value.id = new AST.Id($ID.getText());} | DISTHOLE {$value.isDist = true;} ) '(' (e1=expr {$value.parameters.add($e1.value);}  (',' e2=expr {$value.parameters.add($e2.value);} )*)? ')' )
| DISTXHOLE {$value.isDistX = true;}
;
for_loop returns [AST.ForLoop value]
: 'for' '(' ID 'in' e1=expr ':' e2=expr ')' block {new AST.ForLoop(new AST.Id($ID.getText()), new AST.Range($e1.value, $e2.value), $block.value);};

if_stmt returns [AST.IfStmt value]
@init {$value = new AST.IfStmt();}
: 'if' '(' e=expr {$value.condition = $e.value;} ')' b1=block {$value.trueBlock=$b1.value;} ('else' b2=block {$value.elseBlock = $b2.value;})? ;

assign returns [AST.AssignmentStatement value]
: e1=expr '=' e2=expr {$value = new AST.AssignmentStatement($e1.value, $e2.value); }
;

decl returns [AST.Decl value]
@init {$value = new AST.Decl();}
: dtype {$value.dtype=$dtype.value;} ID {$value.id = new AST.Id($ID.getText());} ('[' dims {$value.dims=$dims.value;} ']')?
//| function_decl
;


statement returns [AST.Statement value]
@init {AST.Annotation at = null;}
: (annotation {at = $annotation.value;})?
  assign  {$value = $assign.value;}
| if_stmt {$value = $if_stmt.value;}
| for_loop {$value = $for_loop.value;}
| function_call {$value = new AST.FunctionCallStatement($function_call.value); }
| decl {$value= $decl.value;}
{$value.annotation = at;}
;

block returns [AST.Block value]
@init {$value=new AST.Block();}
: '{' (s=statement {$value.statements.add($s.value);})* '}'
| s=statement {$value.statements.add($s.value);};
//
//// function definitions
//fparam : return_or_param_type ID;
//fparams : fparam (',' fparam)*;
//
//return_or_param_type: dtype | 'void' | ( dtype ('[' ']')+ ) ;
//function_decl: return_or_param_type ID '(' fparams? ')' block;
//
expr returns [AST.Expression value]
:
    expr '\''         {$value = new AST.Transpose($expr.value);}
    | e1=expr '?' e2=expr ':' e3=expr {$value = new AST.TernaryIf($e1.value, $e2.value, $e3.value);}

    | e1=expr '^' e2=expr     {$value = new AST.ExponOp($e1.value, $e2.value);}
    | e1=expr '/' e2=expr     {$value = new AST.DivOp($e1.value, $e2.value);}
    | e1=expr '*' e2=expr     {$value = new AST.MulOp($e1.value, $e2.value);}
    | e1=expr '+'  e2=expr    {$value = new AST.AddOp($e1.value, $e2.value);}
    | e1=expr '-' e2=expr    {$value = new AST.MinusOp($e1.value, $e2.value);}
//    | expr './' expr    #vecdivop
//    | expr '.*' expr    #vecmulop
    | e1=expr '<' e2=expr     {$value = new AST.LtOp($e1.value, $e2.value);}
    | e1=expr '<=' e2=expr    {$value = new AST.LeqOp($e1.value, $e2.value);}
    | e1=expr '>' e2=expr     {$value = new AST.GtOp($e1.value, $e2.value);}
    | e1=expr '>=' e2=expr    {$value = new AST.GeqOp($e1.value, $e2.value);}
    | e1=expr '!=' e2=expr    {$value = new AST.NeOp($e1.value, $e2.value);}
    | e1=expr '==' e2=expr    {$value = new AST.EqOp($e1.value, $e2.value);}
    | e1=expr '&&' e2=expr    {$value = new AST.AndOp($e1.value, $e2.value);}
    | e1=expr '||' e2=expr    {$value = new AST.OrOp($e1.value, $e2.value);}
//    | expr '[' expr ':' expr ']' #subset
    | function_call         {$value = $function_call.value;}
    | '-' expr              {$value = new AST.UnaryExpression($expr.value);}
    | '(' expr ')'          {$value = $expr.value;}
    | n=number              {$value = $n.value;}
    | id=ID                 {$value = new AST.Id($id.getText()); }
    | ID '[' dims ']'       {$value = new AST.ArrayAccess(new AST.Id($ID.getText()), $dims.value);}
    | st=STRING             {$value = new AST.StringValue($st.getText());}
    | CONSTHOLE             {$value = new AST.ConstHole();}
    ;

query returns  [AST.Query value]
@init {$value = new AST.Query();}
: ('posterior'  {$value.queryType = AST.QueryType.POSTERIOR;}
| 'expectation' {$value.queryType = AST.QueryType.EXPECTATION;})
'(' d=ID ')'      {$value.id = $d.getText();}
;

template returns [AST.Program value]
@init{ $value = new AST.Program(); }
:
(
  d=data          {$value.addData($d.value);}
| st=statement    {$value.addStatement($st.value);}
| q=query         {$value.addQuery($q.value);}
)* ;


// lexer rules

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