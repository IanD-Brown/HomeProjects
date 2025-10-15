#pragma once
#include <map>

#include "solver.h"

class day5Solver : public solver {
private:
    std::multimap<int, int> m_orderRules;
    std::vector<std::vector<int>> m_updatePages;
    solveResult compute();
    solveResult compute2();

    void clearData();

    void loadTestData();

    void loadData(const std::string& line);

protected:

public:
    day5Solver(const std::string& testFile);
};

