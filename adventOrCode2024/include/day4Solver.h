#pragma once
#include "solver.h"

class day4Solver : public solver {
private:
    std::vector<std::string> m_data;
    solveResult compute();
    solveResult compute2();
    void test();
    void test2();

    void clearData();

    void loadTestData();

    void loadData(const std::string& line);

    long search(int row, int col, int index);

protected:

public:
    day4Solver(const std::string& testFile);
};

