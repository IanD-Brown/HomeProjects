#pragma once

#include <map>
#include "solver.h"

class day20Solver : public solver {
private:
	std::vector<std::string> m_data;
	coordinate m_start;
	coordinate m_end;

public:
	day20Solver(const std::string& testFile);

    virtual solveResult compute();

    virtual void clearData();

    virtual void loadTestData();

    virtual void loadData(const std::string& line);
};