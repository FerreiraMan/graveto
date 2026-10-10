DELETE FROM categories
WHERE name IN ('FindAllTxIT-root', 'FindAllTxIT-child', 'FindAllTxIT-grandChild', 'FindAllTxIT-sibling',
               'FindAllTxIT-systemParent', 'FindAllTxIT-treeAccountChild', 'FindAllTxIT-otherAccountChild');
