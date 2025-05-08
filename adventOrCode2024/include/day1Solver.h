#pragma once

#include <vector>
#include "solver.h"

class day1Solver : public solver {
    private:
        std::vector<int> m_left;
        std::vector<int> m_right;
        solveResult compute();
        solveResult compute2();

    public:
        day1Solver(const std::string& testFile);

        void clearData();

        void loadData(const std::string &line);

        void test();
        void test2();
        void loadTestData();
};