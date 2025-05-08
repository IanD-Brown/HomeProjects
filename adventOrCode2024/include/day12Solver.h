#pragma once
#include <map>
#include <set>

#include "solver.h"

class day12Solver : public solver {
private:
	friend struct WallWalker;
	struct Region {
		enum Direction { UP, DOWN, LEFT, RIGHT};
		std::vector < std::set<size_t>> m_cells;

		bool addCell(size_t r, size_t c);
		size_t getCount() const;

		bool hasCellBefore(size_t r, size_t c) const;
		bool hasCellAfter(size_t r, size_t c) const;
		bool hasCellBelow(size_t r, size_t c) const;
		bool hasCellAbove(size_t r, size_t c) const;

		size_t cellCount() const;
	};

	struct plantArea {
		std::vector<std::vector<size_t>> m_locations;
		
		plantArea(size_t r, size_t c);
		void add(size_t r, size_t c);
		std::vector<Region> getRegions(size_t rowCount, size_t colCount);
	};
	size_t m_rowCount;
	size_t m_colCount;
	std::map<char, plantArea*> m_data;

public:
	day12Solver(const std::string& testFile);

    virtual solveResult compute();
    virtual solveResult compute2();

    virtual void clearData();

    virtual void loadTestData();

    virtual void loadData(const std::string& line);
};

