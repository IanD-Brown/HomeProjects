#pragma once

#include "solver.h"
#include <string>
#include <vector>

class day24Solver : public solver {
private:
	std::vector<std::string> m_data;


public:
	day24Solver(const std::string& testFile);

    virtual solveResult compute();

    virtual void clearData();

    virtual void loadTestData();

    virtual void loadData(const std::string& line);
};