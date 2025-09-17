#pragma once

#include "solver.h"
#include <string>
#include <vector>
#include <cstdint>

class day25Solver : public solver {
private:
	std::vector<std::string> m_data;

public:
	day25Solver(const std::string& testFile);

    virtual solveResult compute();

    virtual void clearData();

    virtual void loadTestData();

    virtual void loadData(const std::string& line);
};