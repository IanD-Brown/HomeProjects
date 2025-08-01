#pragma once

#include "solver.h"
#include <set>

class day15Solver : public solver {
private:
	std::vector<std::string> m_warehouse;
	std::string m_moves;
	coordinate m_position;

	size_t countBoxes() const;
	void transformWarehouse();
	void changeColumn(int adjust);
	void changeRow(int adjust);
	bool changeRow(int adjust, size_t row, const std::set<size_t>& columns);
	bool isBox(char c) const;

public:
	day15Solver(const std::string& testFile);

    virtual solveResult compute();

    virtual void clearData();

    virtual void loadTestData();

    virtual void loadData(const std::string& line);
};

