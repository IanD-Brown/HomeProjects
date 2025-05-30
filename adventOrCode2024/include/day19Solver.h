#pragma once

#include <map>
#include "solver.h"

class day19Solver : public solver {
private:
	std::vector<std::string> m_data;
	std::map<char, std::vector<std::string>> m_patterns;

	void addPattern(const std::string &pattern);

	solveResult matchString(const std::string &data, size_t pos,
							std::map<std::string, solveResult> &endCache) const;

public:
	day19Solver(const std::string& testFile);

    virtual solveResult compute();

    virtual void clearData();

    virtual void loadTestData();

    virtual void loadData(const std::string& line);
};