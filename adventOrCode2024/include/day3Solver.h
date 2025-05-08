#pragma once

#include <vector>
#include "solver.h"

class day3Solver : public solver {
    private:
        std::string m_data;
        solveResult compute();
        solveResult compute2();

    protected:
        void test();
        void test2();


    public:
        day3Solver(const std::string& testFile);

        void clearData();

        void loadData(const std::string &line);
};