#pragma once

#include "solver.h"

class day10Solver : public solver {
private:
	std::vector<std::vector<short>> m_data;

public:
	day10Solver(const std::string& testFile);

    virtual solveResult compute();
    virtual solveResult compute2();

    virtual void clearData();

    virtual void loadTestData();

    virtual void loadData(const std::string& line);
};

