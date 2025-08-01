#pragma once

#include "solver.h"

class day23Solver : public solver {
private:
	std::vector<std::string> m_data;

public:
	day23Solver(const std::string& testFile);

    virtual solveResult compute();

    virtual void clearData();

    virtual void loadTestData();

    virtual void loadData(const std::string& line);
};