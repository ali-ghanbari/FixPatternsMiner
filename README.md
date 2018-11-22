    FIELD_ACCESS_TO_METHOD_CALL_MUTATOR                                 *DONE
    METH_CALL_RES_GUARD_MUTATOR                                         IGNORED      GumTree formulation looks complicated
    CASE_BREAKER_MUTATOR                                                *DONE
    ARG_LIST_MUTATOR                                                    *DONE
    AOD                                                                 *DONE        Generalized to binary operators
    MATH_MUTATOR                                                        *DONE        Generalized to binary operators
    AOR                                                                 *DONE        Generalized to binary operators
    CONDITIONALS_BOUNDARY                                               *DONE        Generalized to binary operators
    ROR                                                                 *DONE        Generalized to binary operators
    FIELD_NAME_MUTATOR                                                  *DONE
    LOCAL_NAME_MUTATOR                                                  *DONE
    ARR_LEN_TO_LOCAL_ACC_MUTATOR                                        IGNORED      Not sure how to handle using GumTree
    FIELD_TO_LOCAL_ACC_MUTATOR                                          *DONE
    LOCAL_TO_FIELD_ACCESS_MUTATOR                                       *DONE
    DEREF_GUARD_MUTATOR                                                 *DONE       (e != null) ? e.field : def-val/some-local/some-field
    NON_VOID_METH_CALL_GUARD_MUTATOR                                    *DONE       e0.meth(e1,...,en) is mutated to e0 != null ? e0.meth(e1,...,en)
    RET_DEREF_GUARD_MUTATOR                                             *DONE       if (e == null) { return def-val/some-local/some-field;} e.field
    RET_METH_CALL_GUARD_MUTATOR                                         *DONE       if (e0 == null) { return def-val/some-local/some-field; } e0.meth(e1,...,en)
    VOID_METH_CALL_GUARD_MUTATOR                                        *DONE
    INVERT_NEGS                                                         *DONE
    INLINE_CONSTS                                                       *DONE
    VOID_METHOD_CALLS                                                   *DONE
    NEGATE_CONDITIONALS                                                 *DONE        ** Different from ROR: bool_exp --> !bool_exp // overlap with ROR resolved
    INCREMENTS                                                          *DONE        ** special case handled using UnaryOperatorReplacement
    REMOVE_INCREMENTS                                                   *DONE
    REMOVE_CONDITIONALS_EQ_IF + REMOVE_CONDITIONALS_ORD_IF              *DONE        extract if
    REMOVE_CONDITIONALS_EQ_ELSE + REMOVE_CONDITIONALS_ORD_ELSE          *DONE        extract else (if any)/remove if statement
    EXPERIMENTAL_MEMBER_VARIABLE                                        *DONE
    REMOVE_SWITCH                                                       *DONE        removal of a case clause
    RETURN_VALS + CONSTRUCTOR_CALLS + TRUE_RETURNS +
        FALSE_RETURNS + PRIMITIVE_RETURNS + NULL_RETURNS
        + NON_VOID_METHOD_CALLS                                         DONE        Generalized by "constantification" exp --> const
    EMPTY_RETURNS                                                       IGNORED
    EXPERIMENTAL_SWITCH                                                 IGNORED
    EXPERIMENTAL_ARGUMENT_PROPAGATION + EXPERIMENTAL_NAKED_RECEIVER     DONE