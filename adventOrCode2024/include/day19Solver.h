#pragma once

#include "solver.h"

class day19Solver : public solver {
private:
	std::vector<std::string> m_data;

public:
	day19Solver(const std::string& testFile);

    virtual solveResult compute();

    virtual void clearData();

    virtual void loadTestData();

    virtual void loadData(const std::string& line);
};