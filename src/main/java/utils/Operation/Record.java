package utils.Operation;

import grammar.AST;
import grammar.cfg.Statement;

public class Record {
    OperationType operationType;
    Statement source;
    Statement target; /* empty for delete*/
}
