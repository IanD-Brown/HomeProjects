#pragma once

#include "solver.h"

class day9Solver : public solver {
private:
	std::string m_data;

public:
	day9Solver(const std::string& testFile);

    virtual solveResult compute();
    virtual solveResult compute2();

    virtual void clearData();

    virtual void loadTestData();

    virtual void loadData(const std::string& line);
};

