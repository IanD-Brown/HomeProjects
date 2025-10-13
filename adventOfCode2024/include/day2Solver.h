#pragma once

#include <vector>
#include "solver.h"

class day2Solver : public solver {
    private:
        std::vector<std::vector<int>> m_data;
        solveResult compute();
        solveResult compute2();

    public:
        day2Solver(const std::string& testFile);

        void clearData();

        void loadData(const std::string &line);

        void loadTestData();
};